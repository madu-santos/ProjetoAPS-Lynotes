package br.ufpb.dcx.projetos.musica;

import br.ufpb.dcx.projetos.album.AlbumService;
import br.ufpb.dcx.projetos.exceptions.ResourceNotFoundException;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/musicas")
public class MusicaController {

    @Autowired
    private MusicaService musicaService;

    @Autowired
    private AlbumService albumService; // To provide albums for selection in the form

    @GetMapping
    public String listMusicas(Model model) {
        List<Musica> musicas = musicaService.findAll();
        model.addAttribute("musicas", musicas);
        return "musicas/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("musica", new Musica());
        model.addAttribute("albums", albumService.findAll()); // Provide list of albums for dropdown
        return "musicas/form";
    }

    @PostMapping
    public String createMusica(@Valid @ModelAttribute("musica") Musica musica, BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            model.addAttribute("albums", albumService.findAll()); // Re-populate albums for dropdown on error
            return "musicas/form";
        }
        musicaService.save(musica);
        redirectAttributes.addFlashAttribute("message", "Música criada com sucesso!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/musicas";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Musica musica = musicaService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Música não encontrada para edição com id: " + id));
        model.addAttribute("musica", musica);
        model.addAttribute("albums", albumService.findAll()); // Provide list of albums for dropdown
        return "musicas/form";
    }

    @PostMapping("/edit/{id}")
    public String updateMusica(@PathVariable Long id, @Valid @ModelAttribute("musica") Musica musica, BindingResult result, RedirectAttributes redirectAttributes, Model model) {
        if (result.hasErrors()) {
            // Preserve the ID for the form action if validation fails
            musica.setId(id);
            model.addAttribute("albums", albumService.findAll()); // Re-populate albums for dropdown on error
            return "musicas/form";
        }
        musicaService.update(id, musica);
        redirectAttributes.addFlashAttribute("message", "Música atualizada com sucesso!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/musicas";
    }

    @PostMapping("/delete/{id}")
    public String deleteMusica(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        musicaService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Música excluída com sucesso!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/musicas";
    }
}