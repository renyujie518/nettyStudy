package nio;


import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * @author renyujie518
 * @version 1.0.0
 * @ClassName TestFilesWalkFileTree.java
 * @Description 文件目录遍历    典型的访问者模式
 * @createTime 2022年03月29日 15:28:00
 */
public class TestFilesWalkFileTree {
    public static void main(String[] args) throws IOException {
        AtomicInteger dirCount = new AtomicInteger();
        AtomicInteger fileCount = new AtomicInteger();
        AtomicInteger imlCount = new AtomicInteger();
        Files.walkFileTree(Paths.get("/Users/renyujie/Desktop/nettyStudy"), new SimpleFileVisitor<Path>(){
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
                System.out.println("====>"+dir);
                //匿名内部类  文件夹累加器+1
                dirCount.incrementAndGet();
                return super.preVisitDirectory(dir, attrs);
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
                System.out.println(file);
                //匿名内部类  文件累加器+1
                fileCount.incrementAndGet();
                if (file.toString().endsWith(".iml")) {
                    imlCount.incrementAndGet();
                    System.out.println("发现.xml文件" + file);

                }
                return super.visitFile(file, attrs);
            }
        });
        System.out.println("dir count:" +dirCount);
        System.out.println("file count:" +fileCount);
        System.out.println(".iml count:" +imlCount);
    }

}
