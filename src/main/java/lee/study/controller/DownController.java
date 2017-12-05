package lee.study.controller;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import lee.study.HttpDownServer;
import lee.study.down.HttpDown;
import lee.study.down.HttpDownCallback;
import lee.study.form.DownForm;
import lee.study.model.ChunkInfo;
import lee.study.model.HttpDownInfo;
import lee.study.model.TaskInfo;
import lee.study.util.HttpDownUtil;
import lee.study.ws.HttpDownProgressHandle;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/api")
public class DownController {

  @RequestMapping("/getTask")
  @ResponseBody
  public TaskInfo getTask(@RequestParam String id) {
    return HttpDownServer.DOWN_CONTENT.get(id).getTaskInfo();
  }

  @RequestMapping("/getTaskList")
  @ResponseBody
  public List<TaskInfo> getTaskList() {
    List<TaskInfo> taskInfoList = null;
    if (HttpDownServer.DOWN_CONTENT != null && HttpDownServer.DOWN_CONTENT.size() > 0) {
      taskInfoList = new ArrayList<>();
      for (Object key : HttpDownServer.DOWN_CONTENT.keySet().stream().sorted().toArray()) {
        HttpDownInfo httpDownModel = HttpDownServer.DOWN_CONTENT.get(key);
        if (httpDownModel.getTaskInfo().getStatus() != 0) {
          taskInfoList.add(httpDownModel.getTaskInfo());
        }
      }
    }
    return taskInfoList;
  }

  @RequestMapping("/startTask")
  @ResponseBody
  public String startTask(@RequestBody DownForm downForm) {
    try {
      HttpDownInfo httpDownModel = HttpDownServer.DOWN_CONTENT.get(downForm.getId());
      TaskInfo taskInfo = httpDownModel.getTaskInfo();
      taskInfo.setFilePath(downForm.getPath());
      taskInfo.setConnections(downForm.getConnections());
      //计算chunk列表
      List<ChunkInfo> chunkInfoList = new ArrayList<>();
      for (int i = 0; i < downForm.getConnections(); i++) {
        ChunkInfo chunkInfo = new ChunkInfo();
        chunkInfo.setIndex(i);
        long chunkSize = taskInfo.getTotalSize() / downForm.getConnections();
        chunkInfo.setStartPosition(i * chunkSize);
        if (i == downForm.getConnections() - 1) { //最后一个连接去下载多出来的字节
          chunkSize += taskInfo.getTotalSize() % downForm.getConnections();
        }
        chunkInfo.setEndPosition(chunkInfo.getStartPosition() + chunkSize - 1);
        chunkInfo.setTotalSize(chunkSize);
        chunkInfoList.add(chunkInfo);
      }
      taskInfo.setChunkInfoList(chunkInfoList);
      HttpDownUtil.serialize(httpDownModel,
          httpDownModel.getTaskInfo().getFilePath() + File.separator
              + httpDownModel.getTaskInfo().getFileName() + ".cfg");
      HttpDown.fastDown(httpDownModel, new HttpDownCallback() {

        @Override
        public void start(TaskInfo taskInfo) {
          //标记为下载中并记录开始时间
          HttpDownProgressHandle.sendMsg("start", taskInfo);
        }

        @Override
        public void chunkStart(TaskInfo taskInfo, ChunkInfo chunkInfo) {

        }

        @Override
        public void progress(TaskInfo taskInfo, ChunkInfo chunkInfo) {

        }

        @Override
        public void error(TaskInfo taskInfo, ChunkInfo chunkInfo, Throwable cause) {

        }

        @Override
        public void chunkDone(TaskInfo taskInfo, ChunkInfo chunkInfo) {
          HttpDownProgressHandle.sendMsg("chunkDone", taskInfo);
        }

        @Override
        public void done(TaskInfo taskInfo) {
          HttpDownProgressHandle.sendMsg("done", taskInfo);
        }
      });
    } catch (Exception e) {
      e.printStackTrace();
      return "N";
    }
    return "Y";
  }

  public static void main(String[] args) {
    long size = 103;
    int connections = 6;
    long chunkSize = size / connections;
    System.out.println(chunkSize);
    System.out.println(size % connections);
  }
}
