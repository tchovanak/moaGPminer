/**
 * [CluStream_kMeans.java]
 * CluStream with k-means as macroclusterer
 * 
 * Appeared in seminar paper "Understanding of Internal Clustering Validation Measure in Streaming Environment" (Yunsu Kim)
 * for the course "Seminar: Data Mining and Multimedia Retrival" in RWTH Aachen University, WS 12/13
 * 
 * @author Yunsu Kim
 * based on the code of Timm Jansen (moa@cs.rwth-aachen.de)
 * Data Management and Data Exploration Group, RWTH Aachen University
 *
 *    Licensed under the Apache License, Version 2.0 (the "License");
 *    you may not use this file except in compliance with the License.
 *    You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *    Unless required by applicable law or agreed to in writing, software
 *    distributed under the License is distributed on an "AS IS" BASIS,
 *    WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *    See the License for the specific language governing permissions and
 *    limitations under the License.
 *    
 *    
 */

package moa.clusterers.clustream.PPSDM;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.Random;

import moa.cluster.CFCluster;
import moa.cluster.Cluster;
import moa.cluster.Clustering;
import moa.cluster.PPSDM.SphereClusterPPSDM;
import moa.clusterers.AbstractClusterer;
import moa.core.Measurement;
import com.github.javacliparser.IntOption;
import com.yahoo.labs.samoa.instances.DenseInstance;
import com.yahoo.labs.samoa.instances.Instance;
import java.util.Map;
import moa.core.PPSDM.Configuration;
import moa.core.PPSDM.utils.UtilitiesPPSDM;
import org.apache.commons.math3.stat.correlation.PearsonsCorrelation;

/* Changes > Author: Tomas Chovanak
 * Choice of distance metric is now made with parameter from static Configuration 
 * of PPSDM input parameters.
*/
public class WithKmeansPPSDM extends AbstractClusterer {
	
	private static final long serialVersionUID = 1L;

	public IntOption timeWindowOption = new IntOption("horizon",
			'h', "Rang of the window.", 1000);

	public IntOption maxNumKernelsOption = new IntOption(
			"maxNumKernels", 'm',
			"Maximum number of micro kernels to use.", 100);

	public IntOption kernelRadiFactorOption = new IntOption(
			"kernelRadiFactor", 't',
			"Multiplier for the kernel radius", 2);
	
	public IntOption kOption = new IntOption(
			"k", 'k',
			"k of macro k-means (number of clusters)", 5);

	private int timeWindow;
	private long timestamp = -1;
	private ClustreamKernelPPSDM[] kernels;
	private boolean initialized;
	private List<ClustreamKernelPPSDM> buffer; // Buffer for initialization with kNN
	private int bufferSize;
	private double t;
	private int m;
	
	public WithKmeansPPSDM() {
	
	}

	@Override
	public void resetLearningImpl() {
		this.kernels = new ClustreamKernelPPSDM[maxNumKernelsOption.getValue()];
		this.timeWindow = timeWindowOption.getValue();
		this.initialized = false;
		this.buffer = new LinkedList<>();
		this.bufferSize = maxNumKernelsOption.getValue();
		t = kernelRadiFactorOption.getValue();
		m = maxNumKernelsOption.getValue();
	}

	@Override
	public void trainOnInstanceImpl(Instance instance) {
		int dim = instance.numAttributes(); // CHANGED FROM numValues to numAttributes
		timestamp++;
		// 0. Initialize
		if (!initialized) {
			if (buffer.size() < bufferSize) {
				buffer.add(new ClustreamKernelPPSDM(instance, dim, timestamp, t, m));
				return;
			} else {
				for (int i = 0; i < buffer.size(); i++) {
					kernels[i] = new ClustreamKernelPPSDM(new DenseInstance(1.0, buffer.get(i).getCenter()), dim, timestamp, t, m);
				}
	
				buffer.clear();
				initialized = true;
				return;
			}
		}


		// 1. Determine closest kernel
                
		ClustreamKernelPPSDM closestKernel = null;
		double minDistance = Double.MAX_VALUE;
		for ( int i = 0; i < kernels.length; i++ ) {
			//System.out.println(i+" "+kernels[i].getWeight()+" "+kernels[i].getDeviation());
			double distance = distance(instance.toDoubleArray(), kernels[i].getCenterForReading());
			//double distance = distance(instance, kernels[i].getCenterAsInstanceForReading());
                        if (distance < minDistance) {
				closestKernel = kernels[i];
				minDistance = distance;
			}
		}

		// 2. Check whether instance fits into closestKernel
		double radius = 0.0;
		if ( closestKernel.getWeight() == 1 ) {
			// Special case: estimate radius by determining the distance to the
			// next closest cluster
			radius = Double.MAX_VALUE;
			double[] center = closestKernel.getCenterForReading();
                        //Instance center = closestKernel.getCenterAsInstanceForReading();
			for ( int i = 0; i < kernels.length; i++ ) {
				if ( kernels[i] == closestKernel ) {
					continue;
				}

				double distance = distance(kernels[i].getCenterForReading(), center );
                                //double distance = distance(kernels[i].getCenterAsInstanceForReading(), center );
				radius = Math.min( distance, radius );
			}
		} else {
			radius = closestKernel.getRadius();
		}

		if ( minDistance < radius ) {
			// Date fits, put into kernel and be happy
			closestKernel.insert( instance, timestamp );
			return;
		}

		// 3. Date does not fit, we need to free
		// some space to insert a new kernel
		long threshold = timestamp - timeWindow; // Kernels before this can be forgotten

		// 3.1 Try to forget old kernels
		for ( int i = 0; i < kernels.length; i++ ) {
			if ( kernels[i].getRelevanceStamp() < threshold ) {
				kernels[i] = new ClustreamKernelPPSDM( instance, dim, timestamp, t, m );
				return;
			}
		}

		// 3.2 Merge closest two kernels
		int closestA = 0;
		int closestB = 0;
		minDistance = Double.MAX_VALUE;
		for ( int i = 0; i < kernels.length; i++ ) {
			double[] centerA = kernels[i].getCenterForReading();
                        //Instance centerA = kernels[i].getCenterAsInstanceForReading();
			for ( int j = i + 1; j < kernels.length; j++ ) {
				double dist = distance( centerA, kernels[j].getCenterForReading() );
				//double dist = distance(instance, kernels[i].getCenterAsInstanceForReading());
                                if ( dist < minDistance ) {
					minDistance = dist;
					closestA = i;
					closestB = j;
				}
			}
		}
		assert (closestA != closestB);

		kernels[closestA].add( kernels[closestB] );
		kernels[closestB] = new ClustreamKernelPPSDM( instance, dim, timestamp, t,  m );
	}
	
	@Override
	public Clustering getMicroClusteringResult() {
		if (!initialized) {
			return new Clustering(new Cluster[0]);
		}

		ClustreamKernelPPSDM[] result = new ClustreamKernelPPSDM[kernels.length];
		for (int i = 0; i < result.length; i++) {
			result[i] = new ClustreamKernelPPSDM(kernels[i], t, m);
		}

		return new Clustering(result);
	}
	
	@Override
	public Clustering getClusteringResult() {
                if (!initialized) {
                    return new Clustering(new Cluster[0]);
		}
		return (kMeans_rand(kOption.getValue(), getMicroClusteringResult())).get(1);
	}
	
	public Clustering getClusteringResult(Clustering gtClustering) {
		return (kMeans_gta(kOption.getValue(), getMicroClusteringResult(), gtClustering)).get(1);
	}

	public String getName() {
		return "CluStreamWithKMeans " + timeWindow;
	}

	/**
	 * Distance between two vectors.
	 * 
	 * @param pointA
	 * @param pointB
	 * @return dist
	 */
	private static double distance(double[] pointA, double [] pointB) {
              return UtilitiesPPSDM.distanceBetweenVectors(pointA, pointB);
	}
        
	/**
	 * k-means of (micro)clusters, with ground-truth-aided initialization.
	 * (to produce best results) 
	 * 
	 * @param k
	 * @param data
	 * @return (macro)clustering - CFClusters
	 */
        public  List<Clustering> kMeans_gta(int k, Clustering clustering, Clustering gtClustering) {
		ArrayList<CFCluster> microclusters = new ArrayList<CFCluster>();
                for (int i = 0; i < clustering.size(); i++) {
                    if (clustering.get(i) instanceof CFCluster) {
                        microclusters.add((CFCluster)clustering.get(i));
                    } else {
                        System.out.println("Unsupported Cluster Type:" + clustering.get(i).getClass() + ". Cluster needs to extend moa.cluster.CFCluster");
                    }
                }
                int n = microclusters.size();
		assert (k <= n);
		
		/* k-means */
		Random random = new Random(0);
		Cluster[] centers = new Cluster[k];
		int K = gtClustering.size();
		
		for (int i = 0; i < k; i++) {
			if (i < K) {	// GT-aided
				centers[i] = new SphereClusterPPSDM(gtClustering.get(i).getCenter(), 0);
			} else {		// Randomized
				int rid = random.nextInt(n);
				centers[i] = new SphereClusterPPSDM(microclusters.get(rid).getCenter(), 0);
			}
		}
		
		//return cleanUpKMeans(kMeans(k, centers, microclusters), microclusters);
                Clustering kMeansResult = kMeans(k, centers, microclusters);
                Clustering cleanedKMeansResult = cleanUpKMeans(kMeansResult, microclusters);
                List<Clustering> res = new ArrayList<>();
                res.add(kMeansResult);
                res.add(cleanedKMeansResult);
                return res;
	}
	
	/**
	 * k-means of (micro)clusters, with randomized initialization. 
	 * 
	 * @param k
	 * @param data
	 * @return (macro)clustering - CFClusters
	 */
	public  List<Clustering> kMeans_rand(int k, Clustering clustering) {
		
		ArrayList<CFCluster> microclusters = new ArrayList<CFCluster>();
                for (int i = 0; i < clustering.size(); i++) {
                    if (clustering.get(i) instanceof CFCluster) {
                        microclusters.add((CFCluster)clustering.get(i));
                    } else {
                        System.out.println("Unsupported Cluster Type:" + clustering.get(i).getClass() + ". Cluster needs to extend moa.cluster.CFCluster");
                    }
                }
        
                int n = microclusters.size();
		assert (k <= n);
		
		/* k-means */
		Random random = new Random(0);
		Cluster[] centers = new Cluster[k];
		
		for (int i = 0; i < k; i++) {
			int rid = random.nextInt(n);
			centers[i] = new SphereClusterPPSDM(microclusters.get(rid).getCenter(), 0);
		}
		
		//return cleanUpKMeans(kMeans(k, centers, microclusters), microclusters);
                Clustering kMeansResult = kMeans(k, centers, microclusters);
                Clustering cleanedKMeansResult = cleanUpKMeans(kMeansResult, microclusters);
                List<Clustering> res = new ArrayList<>();
                res.add(kMeansResult);
                res.add(cleanedKMeansResult);
                return res;
        }
	
	/**
	 * (The Actual Algorithm) k-means of (micro)clusters, with specified initialization points.
	 * 
	 * @param k
	 * @param centers - initial centers
	 * @param data
	 * @return (macro)clustering - SphereClusters
	 */
	protected static Clustering kMeans(int k, Cluster[] centers, List<? extends Cluster> data) {
		assert (centers.length == k);
		assert (k > 0);

		int dimensions = centers[0].getCenter().length;

		ArrayList<ArrayList<Cluster>> clustering = new ArrayList<ArrayList<Cluster>>();
		for (int i = 0; i < k; i++) {
			clustering.add(new ArrayList<Cluster>());
		}

		while (true) {
			// Assign points to clusters
			for (Cluster point : data) {
				double minDistance = distance(point.getCenter(), centers[0].getCenter());
				int closestCluster = 0;
				for (int i = 1; i < k; i++) {
					double distance = distance(point.getCenter(), centers[i].getCenter());
					if (distance < minDistance) {
						closestCluster = i;
						minDistance = distance;
					}
				}

				clustering.get(closestCluster).add(point);
			}

			// Calculate new centers and clear clustering lists
			SphereClusterPPSDM[] newCenters = new SphereClusterPPSDM[centers.length];
			for (int i = 0; i < k; i++) {
				newCenters[i] = calculateCenter(clustering.get(i), dimensions);
                                newCenters[i].setId(i);
				clustering.get(i).clear();
			}
			
			// Convergence check
			boolean converged = true;
			for (int i = 0; i < k; i++) {
				if (!Arrays.equals(centers[i].getCenter(), newCenters[i].getCenter())) {
					converged = false;
					break;
				}
			}
			
			if (converged) {
				break;
			} else {
				centers = newCenters;
			}
		}

		return new Clustering(centers);
	}
	
	/**
	 * Rearrange the k-means result into a set of CFClusters, cleaning up the redundancies.
	 * 
	 * @param kMeansResult
	 * @param microclusters
	 * @return
	 */
	public static Clustering cleanUpKMeans(Clustering kMeansResult, ArrayList<CFCluster> microclusters) {
		/* Convert k-means result to CFClusters */
		int k = kMeansResult.size();
		CFCluster[] converted = new CFCluster[k];

		for (CFCluster mc : microclusters) {
		    // Find closest kMeans cluster
		    double minDistance = Double.MAX_VALUE;
		    int closestCluster = 0;
		    for (int i = 0; i < k; i++) {
		    	double distance = distance(kMeansResult.get(i).getCenter(), mc.getCenter());
				if (distance < minDistance) {
				    closestCluster = i;
				    minDistance = distance;
				}
		    }

		    // Add to cluster
		    if ( converted[closestCluster] == null ) {
		    	converted[closestCluster] = (CFCluster)mc.copy();
		    } else {
		    	converted[closestCluster].add(mc);
		    }
		}

		// Clean up
		int count = 0;
		for (int i = 0; i < converted.length; i++) {
		    if (converted[i] != null)
			count++;
		}

		CFCluster[] cleaned = new CFCluster[count];
		count = 0;
		for (int i = 0; i < converted.length; i++) {
		    if (converted[i] != null)
			cleaned[count++] = converted[i];
		}

		return new Clustering(cleaned);
	}

	

	/**
	 * k-means helper: Calculate a wrapping cluster of assigned points[microclusters].
	 * 
	 * @param assigned
	 * @param dimensions
	 * @return SphereClusterPPSDM (with center and radius)
	 */
	private static SphereClusterPPSDM calculateCenter(ArrayList<Cluster> assigned, int dimensions) {
		double[] result = new double[dimensions];
		for (int i = 0; i < result.length; i++) {
			result[i] = 0.0;
		}

		if (assigned.size() == 0) {
			return new SphereClusterPPSDM(result, 0.0);
		}

		for (Cluster point : assigned) {
			double[] center = point.getCenter();
			for (int i = 0; i < result.length; i++) {
				result[i] += center[i];
			}
		}

		// Normalize
		for (int i = 0; i < result.length; i++) {
			result[i] /= assigned.size();
		}

		// Calculate radius: biggest wrapping distance from center
		double radius = 0.0;
		for (Cluster point : assigned) {
			double dist = distance(result, point.getCenter());
			if (dist > radius) {
				radius = dist;
			}
		}
		SphereClusterPPSDM sc = new SphereClusterPPSDM(result, radius);
		sc.setWeight(assigned.size());
		return sc;
	}

	
	/** Miscellaneous **/
	
	@Override
	public boolean implementsMicroClusterer() {
		return true;
	}
	
	public boolean isRandomizable() {
		return false;
	}
	
	public double[] getVotesForInstance(Instance inst) {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	protected Measurement[] getModelMeasurementsImpl() {
		throw new UnsupportedOperationException("Not supported yet.");
	}
	
	@Override
	public void getModelDescription(StringBuilder out, int indent) {
		throw new UnsupportedOperationException("Not supported yet.");
	}

    public void trainOnSparseInstance(Map<Integer, Double> umInstance) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
}
