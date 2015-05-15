package ru.compscicenter2015.concurrency;

import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

public class MergeSortClass implements Runnable {
	private static final int MAX_RECURSIVE_DEEP = 8;
	private final MyFixedThreadPool pool;
	private int array[];
	private final int l; //read only
	private final int r; //readl only
	private final int recursiveDeep; //read only
	private Future <?> futureRecursiveLeft;
	private Future <?> futureRecursiveRight;
	
	MergeSortClass(int array[], int l, int r, MyFixedThreadPool pool) {
		this.array = array;
		this.l = l;
		this.r = r;
		this.pool = pool;
		this.recursiveDeep = 0;
	}
	
	private MergeSortClass(int array[], int l, int r, MyFixedThreadPool pool, int recirsiveDeep) {
		this.array = array;
		this.l = l;
		this.r = r;
		this.pool = pool;
		this.recursiveDeep = recirsiveDeep;
	}
	
	synchronized public int[] getArray() {
		return array;
	}
	
	public void merge(int l, int r) {
		synchronized (array) {
			int tempArray[] = new int[r - l + 1];
			int posInLeftPart = l;
			int middle = (l + r) / 2;
			int posInRightPart = middle + 1;
			for (int posInTempArray = 0; posInTempArray < tempArray.length; posInTempArray++) {
				if (posInRightPart > r
						|| (posInLeftPart <= middle && array[posInLeftPart] <= array[posInRightPart])) {
					tempArray[posInTempArray] = array[posInLeftPart++];
				} else {
					tempArray[posInTempArray] = array[posInRightPart++];
				}
			}
			for (int i = l; i <= r; i++)
				array[i] = tempArray[i - l];	
		}
	}
	
	@Override
	public void run() {
		if (r - l <= 0)
			return;
		if (r - l + 1 <= 16 || recursiveDeep >= MAX_RECURSIVE_DEEP) {
			synchronized (array) {
				for (int i = l; i < r; i++)
					for (int j = i + 1; j <= r; j++)
						if (array[i] > array[j]) {
							int temp = array[i];
							array[i] = array[j];
							array[j] = temp;
						}	
			}
		} else {
			int m = (r + l) / 2;
			if (futureRecursiveLeft == null) {
				futureRecursiveLeft = pool.submit(new MergeSortClass(array, l, m, pool, recursiveDeep + 1), pool.getFutureByThread(Thread.currentThread()));
				if (!futureRecursiveLeft.isDone()) {
					pool.waitForChild();
					return;
				}
			}
			if (futureRecursiveRight == null) {
				futureRecursiveRight = pool.submit(new MergeSortClass(array, m + 1, r, pool, recursiveDeep + 1), pool.getFutureByThread(Thread.currentThread()));
				if (!futureRecursiveRight.isDone()) {
					pool.waitForChild();
					return;
				}
			}
			
			try { // уже выполнено, либо какая то ошибка в подзадаче, о чем мы узнаем
				futureRecursiveLeft.get();
			} catch (InterruptedException e) {
				pool.getFutureByThread(Thread.currentThread()).cancel(false);
				return;
			} catch (ExecutionException e) {
				//ошибка в подзадаче
				return;
			}
			try {
				futureRecursiveRight.get();
			} catch (InterruptedException e) {
				pool.getFutureByThread(Thread.currentThread()).cancel(false);
				return;
			} catch (ExecutionException e) {
				return;
			}
			merge(l, r);

		}
	}
	
}
