package gov.nasa.jpl.mbee.mms.controllers;

import gov.nasa.jpl.mbee.mms.domains.DocMergeRequest;
import gov.nasa.jpl.mbee.mms.services.DocMerge;
import io.micronaut.http.HttpResponse;
import io.micronaut.http.annotation.Body;
import io.micronaut.http.annotation.Controller;
import io.micronaut.http.annotation.Header;
import io.micronaut.http.annotation.Post;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/*
merge doc (pes) from one ref to another on same project same server, take into account branch point and whether pe have changed on fromRef
 */
@Controller("/mms-merge-doc")
public class DocMergeController {

    private static Logger logger = LogManager.getLogger(DocMergeController.class);


    @Post
    public HttpResponse<?> mergeDoc(@Body DocMergeRequest request,
                                    @Header("Authorization") Optional<String> auth) {
        try {
            logger.info(request.toString());
            DocMerge dm = new DocMerge();
            return dm.mergeDoc(request, auth);
        } catch (Exception e) {
            logger.error("Failed: ", e);
            return HttpResponse.badRequest();
        }
    }
}
