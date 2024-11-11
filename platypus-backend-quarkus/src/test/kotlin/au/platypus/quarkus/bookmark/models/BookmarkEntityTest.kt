package au.platypus.quarkus.bookmark.models

import io.quarkus.test.junit.QuarkusTest
import jakarta.inject.Inject
import jakarta.persistence.EntityManager
import jakarta.transaction.Transactional
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

@QuarkusTest
class BookmarkEntityTest {

    @Inject
    lateinit var entityManager: EntityManager

    @Test
    @Transactional
    fun testBookmarkEntity() {
        val newBookmark = Bookmark().apply {
            name = "New Bookmark"
            url = "https://new.com"
            displayOrder = 10000
            description = "New description"
        }
        entityManager.persist(newBookmark)
        entityManager.flush()

        val id = newBookmark.id

        val foundBookmark = entityManager.find(Bookmark::class.java, id)
        assertNotNull(foundBookmark)
        assertEquals("New Bookmark", foundBookmark.name)

        entityManager.remove(newBookmark)
        entityManager.flush()

        assertNull(entityManager.find(Bookmark::class.java, id))
    }
}