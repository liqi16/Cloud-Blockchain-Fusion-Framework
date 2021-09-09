package org.hyperledger.fabric.example;

public class SortAlg {
	public static void sort(int[] a,int lo,int hi){
		if(hi<=lo)return;
		int j=partition(a,lo,hi);
		sort(a,lo,j-1);
		sort(a,j+1,hi);
	}

	private static int partition(int []a,int lo,int hi){
		int i=lo,j=hi+1;
		int v=a[lo];
		while(true){
			while(less(a[++i],v)) if(i==hi) break;
			while(less(v,a[--j])) if(j==lo) break;
			if(i>=j)break;
			exch(a,i,j);
		}
		exch(a,lo,j);
		return j;
	}
	// is v < w ?
	private static boolean less(int v, int w) {
		return (v<w);
	}

	// exchange a[i] and a[j]
	private static void exch(int[] a, int i, int j) {
		int  swap = a[i];
		a[i] = a[j];
		a[j] = swap;
	}
}
