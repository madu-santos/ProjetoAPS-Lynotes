package br.ufpb.dcx.projetos.album;

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
@RequestMapping("/albums")
public class AlbumController {

    @Autowired
    private AlbumService albumService;

    @GetMapping
    public String listAlbums(Model model) {
        List<Album> albums = albumService.findAll();
        model.addAttribute("albums", albums);
        return "albums/list";
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("album", new Album());
        return "albums/form";
    }

    @PostMapping
    public String createAlbum(@Valid @ModelAttribute("album") Album album, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            return "albums/form";
        }
        albumService.save(album);
        redirectAttributes.addFlashAttribute("message", "Álbum criado com sucesso!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/albums";
    }

    @GetMapping("/edit/{id}")
    public String showEditForm(@PathVariable Long id, Model model) {
        Album album = albumService.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Álbum não encontrado para edição com id: " + id));
        model.addAttribute("album", album);
        return "albums/form";
    }

    @PostMapping("/edit/{id}")
    public String updateAlbum(@PathVariable Long id, @Valid @ModelAttribute("album") Album album, BindingResult result, RedirectAttributes redirectAttributes) {
        if (result.hasErrors()) {
            // Preserve the ID for the form action if validation fails
            album.setId(id);
            return "albums/form";
        }
        albumService.update(id, album);
        redirectAttributes.addFlashAttribute("message", "Álbum atualizado com sucesso!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/albums";
    }

    @PostMapping("/delete/{id}")
    public String deleteAlbum(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        albumService.deleteById(id);
        redirectAttributes.addFlashAttribute("message", "Álbum excluído com sucesso!");
        redirectAttributes.addFlashAttribute("messageType", "success");
        return "redirect:/albums";
    }
}