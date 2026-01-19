package controllers;

import play.mvc.Controller;
import play.mvc.Result;
import javax.inject.Inject;
import javax.inject.Singleton;

@Singleton
public class MainController extends Controller {

    @Inject
    public MainController() {
    }

    public Result index() {
        return ok(views.html.index.render());
    }

}
