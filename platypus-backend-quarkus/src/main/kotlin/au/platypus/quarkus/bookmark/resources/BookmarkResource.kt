package au.platypus.quarkus.bookmark.resources

import au.platypus.quarkus.bookmark.models.Bookmark
import au.platypus.quarkus.bookmark.services.BookmarkService
import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.inject.Inject
import jakarta.transaction.Transactional
import jakarta.validation.Valid
import jakarta.validation.constraints.Min
import jakarta.ws.rs.*
import jakarta.ws.rs.core.MediaType
import jakarta.ws.rs.core.Response
import jakarta.ws.rs.ext.ExceptionMapper
import jakarta.ws.rs.ext.Provider
import org.eclipse.microprofile.openapi.annotations.Operation
import org.eclipse.microprofile.openapi.annotations.media.Content
import org.eclipse.microprofile.openapi.annotations.media.Schema
import org.eclipse.microprofile.openapi.annotations.responses.APIResponse
import org.jboss.logging.Logger

/**
 * Bookmark REST API Resource.
 */
@Path("/platypus/api/v1/bookmarks")
@Produces(MediaType.APPLICATION_JSON)
@Consumes(MediaType.APPLICATION_JSON)
class BookmarkResource @Inject constructor(
    private val service: BookmarkService
) {

    companion object {
        private val LOGGER = Logger.getLogger(BookmarkResource::class.java.name)
    }

    @POST
    @Operation(summary = "Create a new bookmark", description = "Adds a bookmark to the system")
    @APIResponse(responseCode = "201", description = "Bookmark created", content = [Content(schema = Schema(implementation = Bookmark::class))])
    @APIResponse(responseCode = "400", description = "Invalid bookmark data")
    fun createBookmark(@Valid product: Bookmark): Response {
        LOGGER.infof("Creating bookmark: %s", product)
        val createdBookmark = service.createBookmark(product)
        LOGGER.infof("Bookmark created with ID: %d", createdBookmark.id)
        return Response.status(Response.Status.CREATED).entity(createdBookmark).build()
    }

    @GET
    fun getAllBookmarks(): Response {
        return Response.ok(service.getAllBookmarks()).build()
    }

    @PUT
    @Path("/{id}")
    fun updateBookmark(
        @PathParam("id") id: Long,
        @Valid updatedBookmark: Bookmark
    ): Response {
        val updated = service.updateBookmark(id, updatedBookmark)
        return if (updated != null) {
            Response.ok(updated).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @DELETE
    @Path("/{id}")
    fun deleteBookmark(@PathParam("id") id: Long): Response {
        val deleted = service.deleteBookmark(id)
        return if (deleted) {
            Response.noContent().build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @GET
    @Path("/{id}")
    fun getBookmark(@PathParam("id") id: Long): Response {
        val product = service.getBookmarkById(id)
        return if (product != null) {
            Response.ok(product).build()
        } else {
            Response.status(Response.Status.NOT_FOUND).build()
        }
    }

    @GET
    @Path("/search")
    @Transactional
    fun searchBookmarks(@QueryParam("q") query: String): Response {
        val results = service.search(query)
        return Response.ok(results).build()
    }

    @PUT
    @Path("/{id}/reorder")
    fun reorderBookmark(
        @PathParam("id") id: Long,
        @QueryParam("newIndex") @Min(1) newIndex: Int
    ): Response {
        val reordered = service.reorderBookmark(id, newIndex)
        return Response.ok(reordered).build()
    }

    @Provider
    class ErrorMapper : ExceptionMapper<Exception> {
        @Inject
        lateinit var objectMapper: ObjectMapper

        override fun toResponse(exception: Exception): Response {
            LOGGER.error("Failed to handle request", exception)
            var code = 500
            if (exception is WebApplicationException) {
                code = exception.response.status
            }
            val exceptionJson = objectMapper.createObjectNode()
            exceptionJson.put("exceptionType", exception.javaClass.name)
            exceptionJson.put("code", code)
            if (exception.message != null) {
                exceptionJson.put("error", exception.message)
            }
            return Response.status(code)
                .entity(exceptionJson)
                .build()
        }
    }
}