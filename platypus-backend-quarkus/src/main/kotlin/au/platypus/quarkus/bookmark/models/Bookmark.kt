package au.platypus.quarkus.bookmark.models

import com.fasterxml.jackson.annotation.JsonIdentityInfo
import com.fasterxml.jackson.annotation.ObjectIdGenerators
import io.quarkus.hibernate.orm.panache.kotlin.PanacheCompanion
import io.quarkus.hibernate.orm.panache.kotlin.PanacheEntity
import jakarta.persistence.*
import jakarta.persistence.Index
import jakarta.persistence.Table
import jakarta.validation.constraints.NotBlank
import jakarta.validation.constraints.Pattern
import jakarta.validation.constraints.Size
import org.hibernate.annotations.*
import org.hibernate.annotations.Cache
import org.slf4j.Logger
import org.slf4j.LoggerFactory
import java.time.Instant

/**
 * Bookmark entity (stores bookmark data)
 * - Mapped to a table using Quarkus + Hibernate ORM
 * - ID is automatically managed by extending PanacheEntity
 * - Hibernate second-level cache applied
 */
@Entity
@Table(
    name = "bookmarks",
    indexes = [
        Index(name = "idx_bookmark_name", columnList = "name"),
        Index(name = "idx_bookmark_url", columnList = "url")
    ],
    uniqueConstraints = [UniqueConstraint(columnNames = ["display_order"])] // Add unique constraint
)
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE) // Hibernate second-level cache applied
@JsonIdentityInfo(
    generator = ObjectIdGenerators.PropertyGenerator::class,
    property = "id"
)
class Bookmark : PanacheEntity() {

    companion object : PanacheCompanion<Bookmark> {
        private val logger: Logger = LoggerFactory.getLogger(Bookmark::class.java)

        /**
         * Search by a specific field (single result)
         * @param fieldName The name of the field to search
         * @param value The value to search for
         * @return Bookmark object or null
         */
        fun findByField(fieldName: String, value: Any): Bookmark? =
            try {
                find("$fieldName", value).firstResult()
            } catch (e: Exception) {
                logger.error("Error finding by field: $fieldName", e)
                null
            }

        /**
         * Search by a specific field using LIKE (case-insensitive)
         * @param fieldName The name of the field to search
         * @param value The value to search for
         * @return List of matching Bookmarks
         */
        fun listByFieldLike(fieldName: String, value: String) =
            try {
                list("LOWER($fieldName) LIKE :value", mapOf("value" to "%${value.lowercase()}%"))
            } catch (e: Exception) {
                logger.error("Error listing by field like: $fieldName", e)
                emptyList()
            }

        // Name search methods
        fun findByName(name: String) = findByField("name", name)
        fun listByName(name: String) = list("name", name)
        fun listByNameLike(name: String) = listByFieldLike("name", name)

        // URL search methods
        fun findByUrl(url: String) = findByField("url", url)
        fun listByUrl(url: String) = list("url", url)
        fun listByUrlLike(url: String) = listByFieldLike("url", url)
    }

    /**
     * Bookmark name (unique value)
     * - @NaturalId: Used by Hibernate for performance optimization
     * - @NotBlank: Empty values are not allowed
     * - @Size: Limited to 1-100 characters
     */
    @NaturalId
    @Column(nullable = false, unique = true, length = 100)
    @field:NotBlank(message = "Name cannot be blank")
    @field:Size(min = 1, max = 100, message = "Name must be between 1 and 100 characters")
    var name: String = ""

    /**
     * Bookmark URL (unique value)
     * - @Pattern: Validation using regular expressions
     */
    @Column(nullable = false, unique = true, length = 500)
    @field:NotBlank(message = "URL cannot be blank")
    @field:Pattern(
        // Use a more robust regex if needed
        regexp = "^(https?|ftp)://[a-zA-Z0-9.-]+(?:\\.[a-zA-Z]{2,6})?(:\\d{1,5})?(?:/.*)?$",
        message = "Invalid URL format"
    )
    var url: String = ""

    /**
     * Display order
     * - Default value: 0
     */
    @Column(name = "display_order")
    var displayOrder: Int = 0

    /**
     * Bookmark description (max 1000 characters)
     */
    @Column(length = 1000)
    @field:Size(max = 1000, message = "Description cannot exceed 1000 characters")
    var description: String = ""

    /**
     * Creation time (automatically set)
     */
    @CreationTimestamp
    @Column(updatable = false)
    lateinit var createdAt: Instant

    /**
     * Update time (automatically updated)
     */
    @UpdateTimestamp
    lateinit var updatedAt: Instant

    /**
     * Method automatically executed before saving data
     */
    @PrePersist
    fun prePersist() {
        name = name.trim()
        url = url.trim()
    }

    /**
     * Method automatically executed before updating data
     */
    @PreUpdate
    fun preUpdate() {
        name = name.trim()
        url = url.trim()
    }

    /**
     * Converts object information to a string (for debugging/logging)
     */
    override fun toString(): String {
        return StringBuilder("Bookmark(")
            .append("id=").append(id)
            .append(", name='").append(name).append('\'')
            .append(", url='").append(url).append('\'')
            .append(", displayOrder=").append(displayOrder)
            .append(", description='").append(description).append('\'')
            .append(", createdAt=").append(createdAt)
            .append(", updatedAt=").append(updatedAt)
            .append(')')
            .toString()
    }

    /**
     * Compares object equality
     * - Considers objects equal if their IDs are the same
     */
    override fun equals(other: Any?): Boolean {
        return this === other || (other is Bookmark && id == other.id)
    }

    /**
     * Returns the object's hash code
     * - Generated based on the ID
     */
    override fun hashCode(): Int {
        return id?.hashCode() ?: 0
    }
}