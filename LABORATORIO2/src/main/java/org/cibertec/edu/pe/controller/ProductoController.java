package org.cibertec.edu.pe.controller;

import java.sql.Date;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.http.HttpSession;

import java.time.LocalDate;

import org.cibertec.edu.pe.model.Detalle;
import org.cibertec.edu.pe.model.Producto;
import org.cibertec.edu.pe.model.Venta;
import org.cibertec.edu.pe.repository.IDetalleRepository;
import org.cibertec.edu.pe.repository.IProductoRepository;
import org.cibertec.edu.pe.repository.IVentaRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.SessionAttributes;

@Controller
@SessionAttributes({"carrito", "total"})
public class ProductoController {
	
	
	
	@Autowired
	private IProductoRepository productoRepository;
	@Autowired
	private IVentaRepository ventaRepository;
	@Autowired
	private IDetalleRepository detalleRepository;
	
	@GetMapping("/index")
	public String listado(Model model) {
		List<Producto> lista = new ArrayList<>();
		lista = productoRepository.findAll();
		model.addAttribute("productos", lista);
		return "index";
	}
	

	@GetMapping("/agregar/{idProducto}")
	public String agregar(Model model, @PathVariable(name = "idProducto", required = true) int idProducto) {
	    Producto producto = productoRepository.findById(idProducto).orElse(null);

	    if (producto != null) {
	        // Obtener el carrito de la sesión
	        List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");

	        // Verificar si el producto ya está en el carrito
	        boolean productoExistente = false;
	        for (Detalle detalle : carrito) {
	            if (detalle.getProducto().getIdProducto() == idProducto) {
	                // Si el producto ya está en el carrito, aumentar la cantidad
	                detalle.setCantidad(detalle.getCantidad() + 1);
	                // Actualizar el subtotal basado en la nueva cantidad
	                detalle.setSubtotal(detalle.getProducto().getPrecio() * detalle.getCantidad());
	                productoExistente = true;
	                break;
	            }
	        }

	        if (!productoExistente) {
	            // Si el producto no está en el carrito, agregarlo con cantidad 1
	            Detalle detalle = new Detalle();
	            detalle.setProducto(producto);
	            detalle.setCantidad(1);
	            // Calcular el subtotal inicial
	            detalle.setSubtotal(detalle.getProducto().getPrecio());
	            carrito.add(detalle);
	        }

	        // Actualizar el modelo con el carrito modificado
	        model.addAttribute("carrito", carrito);
	    }

	    // Redirigir a la página del carrito
	    return "redirect:/index";
	}

	
	    
	
	
	@GetMapping("/carrito")
	public String carrito() {
		return "carrito";
	}
	
	
/***********************************************************************************************************/	

	@GetMapping("/pagar")
	public String pagar(HttpSession session, Model model) {
	    // Obtener el carrito de la sesión
	    List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");

	    // Crear una nueva venta y guardarla en la base de datos
	    Venta venta = new Venta();
	    venta.setFechaRegistro(new Date(System.currentTimeMillis()));
	    venta.setMontoTotal(calcularTotal(carrito));
	    venta = ventaRepository.save(venta);

	    // Guardar los detalles de la venta en la base de datos
	    for (Detalle detalle : carrito) {
	        detalle.setVenta(venta);
	        detalleRepository.save(detalle);
	    }

	    session.removeAttribute("carrito");
	    session.removeAttribute("total");

	    model.addAttribute("mensaje", "¡Pago y registro de venta exitosos!");

	    // Limpiar el modelo de carrito y total
	    model.addAttribute("carrito", new ArrayList<Detalle>());
	    model.addAttribute("total", 0.0);

	    return "mensaje"; 
	}

	private double calcularTotal(List<Detalle> carrito) {
	    double total = 0.0;
	    for (Detalle detalle : carrito) {
	        total += detalle.getSubtotal();
	    }
	    return total;
	}

	
	
/**************************************************************************************************************/	
	
	
	
	@PostMapping("/actualizarCarrito")
	public String actualizarCarrito(Model model) {
	  

	    
	    List<Detalle> carrito = (List<Detalle>) model.getAttribute("carrito");

	    
	    double total = 0.0;
	    for (Detalle detalle : carrito) {
	        double subtotal = detalle.getProducto().getPrecio() * detalle.getCantidad();
	        detalle.setSubtotal(subtotal);
	        total += subtotal;
	    }

	
	    model.addAttribute("carrito", carrito);
	    model.addAttribute("total", total);

	   
	    return "redirect:/carrito";
	}
	

	@ModelAttribute("carrito")
	public List<Detalle> getCarrito() {
		return new ArrayList<Detalle>();
	}
	
	@ModelAttribute("total")
	public double getTotal() {
		return 0.0;
	}
	
	
	@GetMapping("/eliminar/{idProducto}")
	public String eliminarDelCarrito(@PathVariable("idProducto") int idProducto, HttpSession session) {
	    List<Detalle> carrito = (List<Detalle>) session.getAttribute("carrito");
	    if (carrito != null) {
	        // Eliminar el producto con el ID especificado
	        carrito.removeIf(detalle -> detalle.getProducto().getIdProducto() == idProducto);

	        // Recalcular el total del carrito
	        double total = carrito.stream().mapToDouble(Detalle::getSubtotal).sum();

	        session.setAttribute("carrito", carrito);
	        session.setAttribute("total", total);
	    }
	    return "redirect:/carrito";
	}

	
}