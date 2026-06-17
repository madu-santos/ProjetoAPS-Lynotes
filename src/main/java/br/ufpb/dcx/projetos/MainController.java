package br.ufpb.dcx.projetos;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MainController {

    @GetMapping("/")
    public String redirectToAlbums() {
        return "redirect:/albums";
    }
}