package au.platypus.quarkus.bookmark.models

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size

@Entity
@Table(
    name = "bookmarks",
    indexes = [
        Index(name = "idx_bookmark_name", columnList = "name"),
        Index(name = "idx_bookmark_url", columnList = "url")
    ]
)
@Cacheable
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator::class,
    property = "id"
)
class Bookmark : PanacheEntity() {
    companion object : PanacheCompanion<Bookmark> {
        // Search by name
        fun findByName(name: String) = find("name", name).firstResult()
        fun listByName(name: String) = list("name", name)
        fun listByNameLike(name: String) = list("name like ?1", "%$name%")

        // Search by url
        fun findByUrl(url: String) = find("url", url).firstResult()
        fun listByUrl(url: String) = list("url", url)
        fun listByUrlLike(url: String) = list("url like ?1", "%$url%")
    }

    @Column(unique = false, nullable = false, length = 100)
    @field:NotBlank(message = "Name cannot be blank")
    @field:Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    lateinit var name: String

    @Column(unique = false, nullable = false, length = 500)
    @field:NotBlank(message = "URL cannot be blank")
    @field:Pattern(
        regexp = "^(https?://)?([\\da-z.-]+)\\.([a-z.]{2,6})[/\\w .-]*/?$",
        message = "Invalid URL format"
    )
    lateinit var url: String

    @Column(name = "display_order")
    var displayOrder: Int = 0

    @Column(length = 1000)
    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    var description: String = ""

    override fun toString(): String {
        return "Bookmark(id=$id, name='$name', url='$url', displayOrder=$displayOrder)"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Bookmark) return false
        return id == other.id
    }

    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}