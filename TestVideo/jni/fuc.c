#include <jni.h>
#include <android/log.h>
#include <android/bitmap.h>
#include <time.h>
#include <math.h>
#include <limits.h>
#include <stdio.h>
#include <stdlib.h>
#include <inttypes.h>
#include <unistd.h>
#include <string.h>

#include <libavutil/avstring.h>
#include <libavutil/pixdesc.h>
#include <libavutil/imgutils.h>
#include <libavutil/samplefmt.h>

#include <libavformat/avformat.h>

#include <libswscale/swscale.h>

#include <libavcodec/avcodec.h>
#include <libavcodec/avfft.h>


#include <libavcodec/avcodec.h>
#include <libavformat/avformat.h>
#include <libavfilter/avfiltergraph.h>
#include <libavfilter/avcodec.h>
#include <libavfilter/buffersink.h>
#include <libavfilter/buffersrc.h>
#include <libavutil/opt.h>
/*for android logs*/
#include "com_example_fucfuc_testJNIapi.h"

#ifdef __cplusplus
extern "C" {
#endif
int Synthesis(char* in, char* out);
#define LOGI(...) ((void)__android_log_print(ANDROID_LOG_INFO, "zhaowoba", __VA_ARGS__))
//------------------------------------------------------------------------------------------------------------------------------------------------------

JNIEXPORT jint JNICALL Java_com_example_fucfuc_testJNIapi_SynthesisVideo(
		JNIEnv * env, jclass cla, jstring videoPath1,
		jstring GenerationPath) {
	 char* input = (*env)->GetStringUTFChars(env, videoPath1, NULL);
	const char* out_filename_v = (*env)->GetStringUTFChars(env, GenerationPath,
	NULL);
	LOGI("%s   %s    %d input  ---\n",input,out_filename_v,strlen(input));
	char  inputArray[strlen(input)];
	strcpy ( inputArray, input);
	char *out_filename = out_filename_v;

	AVOutputFormat *ofmt = NULL;
	//Input AVFormatContext and Output AVFormatContext
	AVFormatContext *ifmt_ctx_v = NULL, *ofmt_ctx = NULL;
	AVPacket pkt;
	int ret, i;
	int audioindex_a = -1, audioindex_out = -1;
	int frame_index = 0;
	av_register_all();

	//Output
	avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename); //init output file
	if (!ofmt_ctx) {
		printf("Could not create output context\n");
		ret = AVERROR_UNKNOWN;
		goto end;
	}
	ofmt = ofmt_ctx->oformat;
	char *d = "|";
	char* in_filename;
	in_filename = strtok(inputArray, d);
	int xi = 0;
	while (in_filename != NULL) {
		LOGI("%s   in_filename  ---\n",in_filename);
		const char * in_filename_a = (char*) malloc(
				strlen(in_filename) + strlen(".h264") + 1); //str1的长度 + str2的长度 + \0;
		strcpy(in_filename_a, in_filename);
		strcat(in_filename_a, ".h264"); //字符串拼接
		Synthesis(in_filename, in_filename_a);//分离h264 码流
		audioindex_a = -1;
		audioindex_out = 0;
		if ((ret = avformat_open_input(&ifmt_ctx_v, in_filename_a, 0, 0)) < 0) { //open audio file
			printf("Could not open input file.");
			goto end;
		}
		if ((ret = avformat_find_stream_info(ifmt_ctx_v, 0)) < 0) { //find audio stream info
			printf("Failed to retrieve input stream information");
			goto end;
		}

		for (i = 0; i < ifmt_ctx_v->nb_streams; i++) {
			//Create output AVStream according to input AVStream
			if (ifmt_ctx_v->streams[i]->codec->codec_type
					== AVMEDIA_TYPE_VIDEO) {
				AVStream *in_stream = ifmt_ctx_v->streams[i];
				AVStream *out_stream = avformat_new_stream(ofmt_ctx,
						in_stream->codec->codec);
				audioindex_a = i;
				if (!out_stream) {
					printf("Failed allocating output stream\n");
					ret = AVERROR_UNKNOWN;
					goto end;
				}
				//	audioindex_out = out_stream->index;
				//Copy the settings of AVCodecContext
				if (avcodec_copy_context(out_stream->codec, in_stream->codec)
						< 0) {
					printf(
							"Failed to copy context from input to output stream codec context\n");
					goto end;
				}
				out_stream->codec->codec_tag = 0;
				if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
					out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;

				break;
			}
		}
		//  第一次 时 写入头文件  打开输出文件
		if (xi == 0) {
			if (!(ofmt->flags & AVFMT_NOFILE)) { //Open output file
				if (avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE)
						< 0) {
					printf("Could not open output file '%s'", out_filename);
					goto end;
				}
			}
			//Write file header
			if (avformat_write_header(ofmt_ctx, NULL) < 0) {
				printf("Error occurred when opening output file\n");
				goto end;
			}
		}
		//-----------------------------
		while (1) {
			AVFormatContext *ifmt_ctx;
			int stream_index = 0;
			AVStream *in_stream, *out_stream;

			ifmt_ctx = ifmt_ctx_v;
			stream_index = audioindex_out;

			if (av_read_frame(ifmt_ctx, &pkt) >= 0) { //read AVPacket
				do {
					in_stream = ifmt_ctx->streams[pkt.stream_index];
					out_stream = ofmt_ctx->streams[stream_index];

					if (pkt.stream_index == audioindex_a) {
						//FIX：No PTS (Example: Raw H.264)
						//Simple Write PTS
						if (pkt.pts == AV_NOPTS_VALUE) {
							//Write PTS
							AVRational time_base1 = in_stream->time_base;
							//Duration between 2 frames (us)
							int64_t calc_duration = (double) AV_TIME_BASE
									/ av_q2d(in_stream->r_frame_rate);
							//Parameters
							pkt.pts = (double) (frame_index * calc_duration)
									/ (double) (av_q2d(time_base1)
											* AV_TIME_BASE);
							pkt.dts = pkt.pts;
							pkt.duration = (double) calc_duration
									/ (double) (av_q2d(time_base1)
											* AV_TIME_BASE);
							frame_index++;
						}

						break;
					}
				} while (av_read_frame(ifmt_ctx, &pkt) >= 0);
			} else {
				break;
			}
			//			//Convert PTS/DTS
			pkt.pts =
					av_rescale_q_rnd(pkt.pts, in_stream->time_base,
							out_stream->time_base,
							(enum AVRounding) (AV_ROUND_NEAR_INF
									| AV_ROUND_PASS_MINMAX));

			pkt.dts =
					av_rescale_q_rnd(pkt.dts, in_stream->time_base,
							out_stream->time_base,
							(enum AVRounding) (AV_ROUND_NEAR_INF
									| AV_ROUND_PASS_MINMAX));
			pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base,
					out_stream->time_base);
			pkt.pos = -1;
			pkt.stream_index = stream_index;

			printf("Write 1 Packet. size:%5d\tpts:%lld\n", pkt.size, pkt.pts);
			//Write
			if (av_interleaved_write_frame(ofmt_ctx, &pkt) < 0) {
				printf("Error muxing packet\n");
				break;
			}
			av_free_packet(&pkt);
		}
		avformat_close_input(&ifmt_ctx_v);
		xi++;
		in_filename = strtok(NULL, d);
		remove(in_filename_a);
	}
	//-----------------------------

	//Write file trailer
	av_write_trailer(ofmt_ctx);
	end: avformat_close_input(&ifmt_ctx_v);
	/* close output */
	if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
		avio_close(ofmt_ctx->pb);
	avformat_free_context(ofmt_ctx);
	if (ret < 0 && ret != AVERROR_EOF) {
		printf("Error occurred.\n");
		return -1;
	}
	return 0;
}


//---------------------------------------ReplaceAudio
JNIEXPORT jint JNICALL Java_com_example_fucfuc_testJNIapi_ReplaceAudio(
		JNIEnv * env, jclass cla,jstring video,jstring audio) {
	AVOutputFormat *ofmt = NULL;
		//Input AVFormatContext and Output AVFormatContext
		AVFormatContext *ifmt_ctx_v = NULL, *ifmt_ctx_a = NULL, *ofmt_ctx = NULL;
		AVPacket pkt;
		int ret, i;
		int videoindex_v = -1, videoindex_out = -1;
		int audioindex_a = -1, audioindex_out = -1;
		int frame_index = 0;
		int64_t cur_pts_v = 0, cur_pts_a = 0;

		const char* in_filename_v = (*env)->GetStringUTFChars(env, video, NULL);
		const char* in_filename_a = (*env)->GetStringUTFChars(env, audio, NULL);

		const char * c2 = (char*)malloc(strlen(in_filename_v) + strlen(".temp.mp4") + 1); //str1的长度 + str2的长度 + \0;
			strcpy(c2,in_filename_v);
			strcat(c2,".temp.mp4");
		const char *out_filename = c2; //Output file URL

LOGI("%s",c2);
		av_register_all();
		//Input
		if ((ret = avformat_open_input(&ifmt_ctx_v, in_filename_v, 0, 0)) < 0) { //open video  file
			printf("Could not open input file.");
			goto end;
		}
		if ((ret = avformat_find_stream_info(ifmt_ctx_v, 0)) < 0) { //find video stream info
			printf("Failed to retrieve input stream information");
			goto end;
		}

		if ((ret = avformat_open_input(&ifmt_ctx_a, in_filename_a, 0, 0)) < 0) { //open audio file
			printf("Could not open input file.");
			goto end;
		}
		if ((ret = avformat_find_stream_info(ifmt_ctx_a, 0)) < 0) { //find audio stream info
			printf("Failed to retrieve input stream information");
			goto end;
		}
		printf("===========Input Information==========\n");
		av_dump_format(ifmt_ctx_v, 0, in_filename_v, 0); //show video info
		av_dump_format(ifmt_ctx_a, 0, in_filename_a, 0); //show audio info
		printf("======================================\n");
		//Output
		avformat_alloc_output_context2(&ofmt_ctx, NULL, NULL, out_filename); //init output file
		if (!ofmt_ctx) {
			printf("Could not create output context\n");
			ret = AVERROR_UNKNOWN;
			goto end;
		}
		ofmt = ofmt_ctx->oformat;

		for (i = 0; i < ifmt_ctx_v->nb_streams; i++) {
			//Create output AVStream according to input AVStream
			if (ifmt_ctx_v->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
				AVStream *in_stream = ifmt_ctx_v->streams[i];
				AVStream *out_stream = avformat_new_stream(ofmt_ctx,
						in_stream->codec->codec);
				videoindex_v = i;
				if (!out_stream) {
					printf("Failed allocating output stream\n");
					ret = AVERROR_UNKNOWN;
					goto end;
				}
				videoindex_out = out_stream->index;
				//Copy the settings of AVCodecContext
				if (avcodec_copy_context(out_stream->codec, in_stream->codec) < 0) {
					printf(
							"Failed to copy context from input to output stream codec context\n");
					goto end;
				}
				out_stream->codec->codec_tag = 0;
				if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
					out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
				break;
			}
		}

		for (i = 0; i < ifmt_ctx_a->nb_streams; i++) {
			//Create output AVStream according to input AVStream
			if (ifmt_ctx_a->streams[i]->codec->codec_type == AVMEDIA_TYPE_AUDIO) {
				AVStream *in_stream = ifmt_ctx_a->streams[i];
				AVStream *out_stream = avformat_new_stream(ofmt_ctx,
						in_stream->codec->codec);
				audioindex_a = i;
				if (!out_stream) {
					printf("Failed allocating output stream\n");
					ret = AVERROR_UNKNOWN;
					goto end;
				}
				audioindex_out = out_stream->index;
				//Copy the settings of AVCodecContext
				if (avcodec_copy_context(out_stream->codec, in_stream->codec) < 0) {
					printf(
							"Failed to copy context from input to output stream codec context\n");
					goto end;
				}
				out_stream->codec->codec_tag = 0;
				if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
					out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;

				break;
			}
		}

		printf("==========Output Information==========\n");
		av_dump_format(ofmt_ctx, 0, out_filename, 1);
		printf("======================================\n");
		if (!(ofmt->flags & AVFMT_NOFILE)) { //Open output file
			if (avio_open(&ofmt_ctx->pb, out_filename, AVIO_FLAG_WRITE) < 0) {
				printf("Could not open output file '%s'", out_filename);
				goto end;
			}
		}
		//Write file header
		if (avformat_write_header(ofmt_ctx, NULL) < 0) {
			printf("Error occurred when opening output file\n");
			goto end;
		}

		while (1) {
			AVFormatContext *ifmt_ctx;
			int stream_index = 0;
			AVStream *in_stream, *out_stream;

			//Get an AVPacket
			if (av_compare_ts(cur_pts_v,
					ifmt_ctx_v->streams[videoindex_v]->time_base, cur_pts_a,
					ifmt_ctx_a->streams[audioindex_a]->time_base) <= 0) {
				ifmt_ctx = ifmt_ctx_v;
				stream_index = videoindex_out;

				if (av_read_frame(ifmt_ctx, &pkt) >= 0) { //read AVPacket
					do {
						in_stream = ifmt_ctx->streams[pkt.stream_index];
						out_stream = ofmt_ctx->streams[stream_index];

						if (pkt.stream_index == videoindex_v) {
							//FIX：No PTS (Example: Raw H.264)
							//Simple Write PTS
							if (pkt.pts == AV_NOPTS_VALUE) {
								//Write PTS
								AVRational time_base1 = in_stream->time_base;
								//Duration between 2 frames (us)
								int64_t calc_duration = (double) AV_TIME_BASE
										/ av_q2d(in_stream->r_frame_rate);
								//Parameters
								pkt.pts = (double) (frame_index * calc_duration)
										/ (double) (av_q2d(time_base1)
												* AV_TIME_BASE);
								pkt.dts = pkt.pts;
								pkt.duration = (double) calc_duration
										/ (double) (av_q2d(time_base1)
												* AV_TIME_BASE);
								frame_index++;
							}

							cur_pts_v = pkt.pts;
							break;
						}
					} while (av_read_frame(ifmt_ctx, &pkt) >= 0);
				} else {
					break;
				}
			} else {
				ifmt_ctx = ifmt_ctx_a;
				stream_index = audioindex_out;
				if (av_read_frame(ifmt_ctx, &pkt) >= 0) {
					do {
						in_stream = ifmt_ctx->streams[pkt.stream_index];
						out_stream = ofmt_ctx->streams[stream_index];

						if (pkt.stream_index == audioindex_a) {

							//FIX：No PTS
							//Simple Write PTS
							if (pkt.pts == AV_NOPTS_VALUE) {
								//Write PTS
								AVRational time_base1 = in_stream->time_base;
								//Duration between 2 frames (us)
								int64_t calc_duration = (double) AV_TIME_BASE
										/ av_q2d(in_stream->r_frame_rate);
								//Parameters
								pkt.pts = (double) (frame_index * calc_duration)
										/ (double) (av_q2d(time_base1)
												* AV_TIME_BASE);
								pkt.dts = pkt.pts;
								pkt.duration = (double) calc_duration
										/ (double) (av_q2d(time_base1)
												* AV_TIME_BASE);
								frame_index++;
							}
							cur_pts_a = pkt.pts;

							break;
						}
					} while (av_read_frame(ifmt_ctx, &pkt) >= 0);
				} else {
					break;
				}

			}

			//Convert PTS/DTS
			pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base,
					out_stream->time_base,
					(enum AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
			pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base,
					out_stream->time_base,
					(enum AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
			pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base,
					out_stream->time_base);
			pkt.pos = -1;
			pkt.stream_index = stream_index;

			printf("Write 1 Packet. size:%5d\tpts:%lld\n", pkt.size, pkt.pts);
			//Write
			if (av_interleaved_write_frame(ofmt_ctx, &pkt) < 0) {
				printf("Error muxing packet\n");
				break;
			}
			av_free_packet(&pkt);

		}
		//Write file trailer
		av_write_trailer(ofmt_ctx);
		end: avformat_close_input(&ifmt_ctx_v);
		avformat_close_input(&ifmt_ctx_a);
		/* close output */
		if (ofmt_ctx && !(ofmt->flags & AVFMT_NOFILE))
			avio_close(ofmt_ctx->pb);
		avformat_free_context(ofmt_ctx);
		if (ret < 0 && ret != AVERROR_EOF) {
			printf("Error occurred.\n");
			return -1;
		}
		remove(in_filename_v);
		rename(c2,in_filename_v);
		return 0;


}
//------------------------------------------------------------------------------------------------------------------------------------------------------


int Synthesis(char* in, char* out) {
	AVOutputFormat *ofmt_v = NULL;
	//（Input AVFormatContext and Output AVFormatContext）
	AVFormatContext *ifmt_ctx = NULL, *ofmt_ctx_v = NULL;
	AVPacket pkt;
	int ret, i;
	int videoindex = -1;
	int frame_index = 0;
LOGI("-----------------------ok---------------------");
	const char *in_filename =in; //Input file URL
	//char *in_filename  = "cuc_ieschool.mkv";
	const char *out_filename_v =
			out;	//Output file URL
	//char *out_filename_a = "cuc_ieschool.mp3";

	av_register_all();
	//Input
	if ((ret = avformat_open_input(&ifmt_ctx, in_filename, 0, 0)) < 0) {
		printf("Could not open input file.");
		goto end;
	}
	if ((ret = avformat_find_stream_info(ifmt_ctx, 0)) < 0) {
		printf("Failed to retrieve input stream information");
		goto end;
	}

	//Output
	avformat_alloc_output_context2(&ofmt_ctx_v, NULL, NULL, out_filename_v);
	if (!ofmt_ctx_v) {
		printf("Could not create output context\n");
		ret = AVERROR_UNKNOWN;
		goto end;
	}
	ofmt_v = ofmt_ctx_v->oformat;

	for (i = 0; i < ifmt_ctx->nb_streams; i++) {
		//Create output AVStream according to input AVStream
		AVFormatContext *ofmt_ctx;
		AVStream *in_stream = ifmt_ctx->streams[i];
		AVStream *out_stream = NULL;

		if (ifmt_ctx->streams[i]->codec->codec_type == AVMEDIA_TYPE_VIDEO) {
			videoindex = i;
			out_stream = avformat_new_stream(ofmt_ctx_v,
					in_stream->codec->codec);
			ofmt_ctx = ofmt_ctx_v;
			if (!out_stream) {
				printf("Failed allocating output stream\n");
				ret = AVERROR_UNKNOWN;
				goto end;
			}
			//Copy the settings of AVCodecContext
			if (avcodec_copy_context(out_stream->codec, in_stream->codec) < 0) {
				printf(
						"Failed to copy context from input to output stream codec context\n");
				goto end;
			}
			out_stream->codec->codec_tag = 0;

			if (ofmt_ctx->oformat->flags & AVFMT_GLOBALHEADER)
				out_stream->codec->flags |= CODEC_FLAG_GLOBAL_HEADER;
		}
	}

	//Dump Format------------------
	printf("\n==============Input Video=============\n");
	av_dump_format(ifmt_ctx, 0, in_filename, 0);
	printf("\n==============Output Video============\n");
	av_dump_format(ofmt_ctx_v, 0, out_filename_v, 1);
	//Open output file
	if (!(ofmt_v->flags & AVFMT_NOFILE)) {
		if (avio_open(&ofmt_ctx_v->pb, out_filename_v, AVIO_FLAG_WRITE) < 0) {
			printf("Could not open output file '%s'", out_filename_v);
			goto end;
		}
	}


	//Write file header
	if (avformat_write_header(ofmt_ctx_v, NULL) < 0) {
		printf("Error occurred when opening video output file\n");
		goto end;
	}
	AVBitStreamFilterContext* h264bsfc = av_bitstream_filter_init(
			"h264_mp4toannexb");

	while (1) {
		AVFormatContext *ofmt_ctx;
		AVStream *in_stream, *out_stream;
		//Get an AVPacket
		if (av_read_frame(ifmt_ctx, &pkt) < 0)
			break;
		in_stream = ifmt_ctx->streams[pkt.stream_index];

		if (pkt.stream_index == videoindex) {
			out_stream = ofmt_ctx_v->streams[0];
			ofmt_ctx = ofmt_ctx_v;
			printf("Write Video Packet. size:%d\tpts:%lld\n", pkt.size,
					pkt.pts);
			av_bitstream_filter_filter(h264bsfc, in_stream->codec, NULL,
					&pkt.data, &pkt.size, pkt.data, pkt.size, 0);
		}  else {
			continue;
		}

		//Convert PTS/DTS
		pkt.pts = av_rescale_q_rnd(pkt.pts, in_stream->time_base,
				out_stream->time_base,
				(enum AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
		pkt.dts = av_rescale_q_rnd(pkt.dts, in_stream->time_base,
				out_stream->time_base,
				(enum AVRounding) (AV_ROUND_NEAR_INF | AV_ROUND_PASS_MINMAX));
		pkt.duration = av_rescale_q(pkt.duration, in_stream->time_base,
				out_stream->time_base);
		pkt.pos = -1;
		pkt.stream_index = 0;
		//Write
		if (av_interleaved_write_frame(ofmt_ctx, &pkt) < 0) {
			printf("Error muxing packet\n");
			break;
		}
		//printf("Write %8d frames to output file\n",frame_index);
		av_free_packet(&pkt);
		frame_index++;
	}

	av_bitstream_filter_close(h264bsfc);

	//Write file trailer
	av_write_trailer(ofmt_ctx_v);
	end: avformat_close_input(&ifmt_ctx);
	/* close output */

	if (ofmt_ctx_v && !(ofmt_v->flags & AVFMT_NOFILE))
		avio_close(ofmt_ctx_v->pb);

	avformat_free_context(ofmt_ctx_v);

	if (ret < 0 && ret != AVERROR_EOF) {
		printf("Error occurred.\n");
		return -1;
	}
	return 0;
}



//----------------------------------------------------------------------------sssssssssssssssssssssssssssssssssss
// video crop 480






int open_input_file(const char *filename);
int init_filters(const char *filters_descr);



 AVFormatContext *fmt_ctx;
 AVCodecContext *dec_ctx;
AVFilterContext *buffersink_ctx;
AVFilterContext *buffersrc_ctx;
AVFilterGraph *filter_graph;
 int video_stream_index = -1;
//------------------------------------
 int open_input_file(const char *filename) {
	int ret;
	AVCodec *dec;

	if ((ret = avformat_open_input(&fmt_ctx, filename, NULL, NULL)) < 0) {
		av_log(NULL, AV_LOG_ERROR, "Cannot open input file\n");
		return ret;
	}

	if ((ret = avformat_find_stream_info(fmt_ctx, NULL)) < 0) {
		av_log(NULL, AV_LOG_ERROR, "Cannot find stream information\n");
		return ret;
	}

	/* select the video stream */
	ret = av_find_best_stream(fmt_ctx, AVMEDIA_TYPE_VIDEO, -1, -1, &dec, 0);//return decoder in dec
	if (ret < 0) {
		av_log(NULL, AV_LOG_ERROR,
				"Cannot find a video stream in the input file\n");
		return ret;
	}
	video_stream_index = ret;
	dec_ctx = fmt_ctx->streams[video_stream_index]->codec;

	//printf("decoder-codec : %s",dec_ctx->codec_id);

	av_opt_set_int(dec_ctx, "refcounted_frames", 1, 0);		//

	/* init the video decoder */
	if ((ret = avcodec_open2(dec_ctx, dec, NULL)) < 0) {
		av_log(NULL, AV_LOG_ERROR, "Cannot open video decoder\n");
		return ret;
	}

	return 0;
}

 int init_filters(const char *filters_descr) {
	char args[512];
	int ret = 0;
	AVFilter *buffersrc = avfilter_get_by_name("buffer");
	AVFilter *buffersink = avfilter_get_by_name("buffersink");
	AVFilterInOut *outputs = avfilter_inout_alloc();
	AVFilterInOut *inputs = avfilter_inout_alloc();
	enum AVPixelFormat pix_fmts[] = { AV_PIX_FMT_YUV420P, AV_PIX_FMT_NONE };//modify by elesos.com 图片格式要正确

	filter_graph = avfilter_graph_alloc();
	if (!outputs || !inputs || !filter_graph) {
		ret = AVERROR(ENOMEM);
		goto end;
	}

	/* buffer video source: the decoded frames from the decoder will be inserted here. */
	snprintf(args, sizeof(args),
			"video_size=%dx%d:pix_fmt=%d:time_base=%d/%d:pixel_aspect=%d/%d",
			dec_ctx->width, dec_ctx->height, dec_ctx->pix_fmt,
			dec_ctx->time_base.num, dec_ctx->time_base.den,
			dec_ctx->sample_aspect_ratio.num, dec_ctx->sample_aspect_ratio.den);

	ret = avfilter_graph_create_filter(&buffersrc_ctx, buffersrc, "in", args,
	NULL, filter_graph);
	if (ret < 0) {
		av_log(NULL, AV_LOG_ERROR, "Cannot create buffer source\n");
		goto end;
	}

	/* buffer video sink: to terminate the filter chain. */
	ret = avfilter_graph_create_filter(&buffersink_ctx, buffersink, "out",
	NULL, NULL, filter_graph);
	if (ret < 0) {
		av_log(NULL, AV_LOG_ERROR, "Cannot create buffer sink\n");
		goto end;
	}

	ret = av_opt_set_int_list(buffersink_ctx, "pix_fmts", pix_fmts,
			AV_PIX_FMT_NONE, AV_OPT_SEARCH_CHILDREN);
	if (ret < 0) {
		av_log(NULL, AV_LOG_ERROR, "Cannot set output pixel format\n");
		goto end;
	}

	/* Endpoints for the filter graph. */
	outputs->name = av_strdup("in");
	outputs->filter_ctx = buffersrc_ctx;
	outputs->pad_idx = 0;
	outputs->next = NULL;

	inputs->name = av_strdup("out");
	inputs->filter_ctx = buffersink_ctx;
	inputs->pad_idx = 0;
	inputs->next = NULL;

	if ((ret = avfilter_graph_parse_ptr(filter_graph, filters_descr, &inputs,
			&outputs, NULL)) < 0)
		goto end;

	if ((ret = avfilter_graph_config(filter_graph, NULL)) < 0)
		goto end;

	end: avfilter_inout_free(&inputs);
	avfilter_inout_free(&outputs);

	return ret;
}


JNIEXPORT jint JNICALL Java_com_example_fucfuc_testJNIapi_cropVideo(JNIEnv *env,
		jclass j, jstring inpath,jstring width) {
	const char* in_file = (*env)->GetStringUTFChars(env, inpath, NULL);
	const char* video_width = (*env)->GetStringUTFChars(env, width, NULL);

     int widthi =atoi(video_width);
    const char *command="crop=";
    LOGI("%d",widthi);

    //const char *filter_descr = "crop=720:720:0:0";
    const char * crop = (char*)malloc(strlen(command) + (strlen(video_width)*2)+5 + 1); //str1的长度 + str2的长度 + \0;
    				strcpy(crop,command);
    				strcat(crop,video_width);
    				strcat(crop,":");
    				strcat(crop,video_width);
    				strcat(crop,":");
    				strcat(crop,"0:0");


	const char *filter_descr = crop;
	const int in_w = widthi, in_h = widthi;	//宽高

	const char * c2 = (char*)malloc(strlen(in_file) + strlen(".temp.mp4") + 1); //str1的长度 + str2的长度 + \0;
				strcpy(c2,in_file);
				strcat(c2,".temp.mp4");
			const char *out_file = c2; //Output file URL


	LOGI("%s",in_file);
	int ret;
	AVPacket packet;
	AVFrame *frame = av_frame_alloc();		//解码后的帧
	AVFrame *filt_frame = av_frame_alloc();		//filter后的帧
	int got_frame;
	//-------------
	static AVFormatContext* pFormatCtx;
	static AVOutputFormat* fmt;
	static AVStream* video_st;
	static AVCodecContext* pCodecCtx;
	static AVCodec* pCodec;

	av_register_all();
	avfilter_register_all();
	pFormatCtx = avformat_alloc_context();
	//猜格式
	fmt = av_guess_format(NULL, out_file, NULL);
	pFormatCtx->oformat = fmt;

	if (avio_open(&pFormatCtx->pb, out_file, AVIO_FLAG_READ_WRITE) < 0) {
		printf("输出文件打开失败");
		return -1;
	}

	video_st = avformat_new_stream(pFormatCtx, 0);
	if (video_st == NULL) {
		return -1;
	}

	pCodecCtx = video_st->codec;
	pCodecCtx->codec_id = fmt->video_codec;
	pCodecCtx->codec_type = AVMEDIA_TYPE_VIDEO;
	pCodecCtx->pix_fmt = PIX_FMT_YUV420P;
	pCodecCtx->width = in_w;
	pCodecCtx->height = in_h;
	pCodecCtx->time_base.num = 1;
	pCodecCtx->time_base.den = 25;
	pCodecCtx->bit_rate = 400000;
	pCodecCtx->gop_size = 250;
	pCodecCtx->qmin = 10;
	pCodecCtx->qmax = 51;
	//输出格式信息
	av_dump_format(pFormatCtx, 0, out_file, 1);

	pCodec = avcodec_find_encoder(pCodecCtx->codec_id);
	if (!pCodec) {
		printf("find decode error\n");
		return -1;
	}
	if (avcodec_open2(pCodecCtx, pCodec, NULL) < 0) {
		printf("open decode error\n");
		return -1;
	}
	if (!frame || !filt_frame) {
		perror("Could not allocate frame");
		exit(1);
	}

	if ((ret = open_input_file(in_file)) < 0)
		goto end;
	if ((ret = init_filters(filter_descr)) < 0)		//初始化filter
		goto end;

	//----------------
	//write_header
	avformat_write_header(pFormatCtx, NULL);
	/* read all packets */
	int x = 0;
	while (1) {
		if ((ret = av_read_frame(fmt_ctx, &packet)) < 0)    //从输入文件读包
			break;
		if (packet.stream_index == video_stream_index) {    //如果是视频包
			x++;
			AVPacket* pkt = &packet;
			printf("original packet : %d   \n", pkt->pts);

			got_frame = 0;
			ret = avcodec_decode_video2(dec_ctx, frame, &got_frame, &packet); //解码
			if (ret < 0) {
				av_log(NULL, AV_LOG_ERROR, "Error decoding video\n");
				break;
			}

			if (got_frame) {    //判断解码后是否得到完整的一帧
				frame->pts = av_frame_get_best_effort_timestamp(frame);

				printf("original frame : %d  \n", frame->pts);

				/* push the decoded frame into the filtergraph */
				if (av_buffersrc_add_frame_flags(buffersrc_ctx, frame,
						AV_BUFFERSRC_FLAG_KEEP_REF) < 0) {
					av_log(NULL, AV_LOG_ERROR,
							"Error while feeding the filtergraph\n");
					break;
				}

				/* pull filtered frames from the filtergraph */
				while (1) {
					ret = av_buffersink_get_frame(buffersink_ctx, filt_frame);
					if (ret == AVERROR(EAGAIN) || ret == AVERROR_EOF)
						break;
					if (ret < 0)
						goto end;

					printf("result filt frame : %d \n", filt_frame->pts);

					AVPacket pkt2;
					pkt2.data = NULL;
					pkt2.size = 0;
					av_init_packet(&pkt2);

					int got_picture = 0;
					//编码
					int ret = avcodec_encode_video2(pCodecCtx, &pkt2,
							filt_frame, &got_picture);
					if (ret < 0) {
						printf("编码错误！\n");
						return -1;
					}
					if (got_picture == 1) {
						int index = video_st->index;
						pkt2.stream_index = index;

						pkt2.dts = av_rescale_q_rnd(pkt2.dts,
								fmt_ctx->streams[video_stream_index]->time_base,
								pFormatCtx->streams[0]->time_base,
								(enum AVRounding)(
										AV_ROUND_NEAR_INF
												| AV_ROUND_PASS_MINMAX));
						pkt2.pts = av_rescale_q_rnd(pkt2.pts,
								fmt_ctx->streams[video_stream_index]->time_base,
								pFormatCtx->streams[0]->time_base,
								(enum AVRounding)(
										AV_ROUND_NEAR_INF
												| AV_ROUND_PASS_MINMAX));

						pkt2.duration = av_rescale_q(pkt2.duration,fmt_ctx->streams[video_stream_index]->time_base,pFormatCtx->streams[0]->time_base);
						ret = av_write_frame(pFormatCtx, &pkt2);
						av_free_packet(&pkt2);
					}
					av_frame_unref(filt_frame);
				}
				av_frame_unref(frame);
			} else {
				printf("no frame got \n");
			}
		}
		av_free_packet(&packet);
	}
	av_write_trailer(pFormatCtx);

	if (video_st) {
		avcodec_close(video_st->codec);
	}
	avio_close(pFormatCtx->pb);
	avformat_free_context(pFormatCtx);

	printf("%d", x);
	end: avfilter_graph_free(&filter_graph);
	avcodec_close(dec_ctx);
	avformat_close_input(&fmt_ctx);
	av_frame_free(&frame);
	av_frame_free(&filt_frame);

	if (ret < 0 && ret != AVERROR_EOF) {
		fprintf(stderr, "Error occurred: %s\n", av_err2str(ret));
		exit(1);
	}
	remove(in_file);
	rename(c2,in_file);
	return 1;
}
#ifdef __cplusplus
}
#endif
