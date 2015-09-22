package com.project.travel.servlet;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.io.* ;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import com.google.appengine.api.blobstore.BlobKey;
import com.google.appengine.api.blobstore.BlobstoreService;
import com.google.appengine.api.blobstore.BlobstoreServiceFactory;

public class FileUploadServlet extends HttpServlet {
    private BlobstoreService blobstoreService = BlobstoreServiceFactory.getBlobstoreService();
    @Override
    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws ServletException, IOException {
	String key = req.getParameter("key");
        Map<String, List<BlobKey>> blobs = blobstoreService.getUploads(req);
        List<BlobKey> blobKeys = blobs.get("file");
	if(key!=null){
	   blobstoreService.delete(new BlobKey(key));
	}
	PrintWriter out = res.getWriter() ;
      	out.println(blobKeys.get(0).getKeyString());
    }
    @Override
    public void doGet(HttpServletRequest req, HttpServletResponse res) throws IOException {
	String key = req.getParameter("blob-key");
	if(key==null){
		PrintWriter out = res.getWriter() ;
      		out.println(blobstoreService.createUploadUrl("/file")) ;
	}else{
		BlobKey blobKey = new BlobKey(key);
        	blobstoreService.serve(blobKey, res);
	}
        
    }
}
