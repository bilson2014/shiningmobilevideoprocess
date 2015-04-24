package com.example.tools;

public class tools {

	public class maintest {
		int[][] bili = new int[4][4];
	
		public void main() {
			int w = 4800, h = 7200;
			init();
			jisuan j=new jisuan(h,w);
			int[] x=j.getValue();
			System.out.print(x[0]+"  "+x[1]);
		}
		
		

		public void init() {
			bili[0][0] = 480;
			bili[0][1] = 720;

			bili[1][0] = 480;
			bili[1][1] = 800;

			bili[2][0] = 480;
			bili[2][1] = 864;

			bili[3][0] = 480;
			bili[3][1] = 640;

		}

		 class jisuan {
			final static int width = 480;
			int inH = 0, inW = 0;

			public jisuan(int inH, int inW) {
				super();
				this.inH = inH;
				this.inW = inW;
			}

			private int getMax(int x, int y) {
				int tmp = 0;
				if (x > y) {
					tmp = y;
				} else {
					tmp = x;
				}
				for (int i = tmp; i >= 1; i--) {
					if (x % i == 0 && y % i == 0) {
						return i;
					}
				}
				return -1;
			}

			public int[] getValue() {
				int[] result = new int[2];
				int res = getMax(inH, inW);
				if (res != -1) {
					inH = inH / res;
					inW = inW / res;

					for (int i = 0; i < bili.length; i++) {
						int r = getMax(bili[i][1], bili[i][0]);
						if (r != -1) {
							int oH = bili[i][1] / r;
							int oW = bili[i][0] / r;
							if (oH == inH && oW == inW) {
								result[0] = bili[i][1];
								result[1] = bili[i][0];
								return result;
							}
						}
					}
				} else {
					System.out.print("fail");
				}
				return null;
			}
		}

	}


}
