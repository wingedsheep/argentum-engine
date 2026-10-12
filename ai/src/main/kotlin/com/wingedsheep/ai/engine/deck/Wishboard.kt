package com.wingedsheep.ai.engine.deck

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.model.CardDefinition
import kotlin.random.Random

/**
 * A generated sideboard — the cards a seat owns *outside the game* (CR 400.11a) — for a seat that
 * never built one: Momir Basic, a random constructed deck, Cube Pool Play.
 *
 * Sealed and draft need nothing like this: their sideboard is the rest of the pool (CR 100.4b),
 * which a random *sealed* deck has too. But a seat whose deck was generated or fixed by the format
 * would otherwise start with an empty sideboard, and every card that reaches outside the game —
 * Learn, Burning Wish, Karn, Ring of Ma'rûf — turns into a blank. The wishboard gives those cards
 * something to find, sized like a constructed sideboard (CR 100.4a: no more than fifteen cards).
 *
 * The slots are picked for what the corpus actually asks for, not at random: **Lessons** first
 * (Learn is far and away the most common way a card reaches outside the game), then one or two of
 * each card type a printed wish names — instant (Cunning Wish), sorcery (Burning Wish), creature and
 * land (Living Wish), artifact and enchantment (Karn, Glittering-style "any card"). A slot the
 * candidate pool can't fill is skipped, and whatever is left over is filled at random, so a pool
 * with no Lessons still yields a full fifteen.
 *
 * Every card is a singleton: a wishboard is a menu, and fifteen different answers beat three
 * copies of one.
 */
object Wishboard {

    /** CR 100.4a — a constructed sideboard holds no more than fifteen cards. */
    const val SIZE = 15

    private data class Slot(val count: Int, val matches: (CardDefinition) -> Boolean)

    private val SLOTS = listOf(
        Slot(3) { it.typeLine.hasSubtype(Subtype.LESSON) },
        Slot(2) { it.isInstant },
        Slot(2) { it.isSorcery },
        Slot(2) { it.isCreature },
        Slot(1) { it.isArtifact && !it.isCreature },
        Slot(1) { it.isEnchantment && !it.isCreature },
        Slot(1) { it.isLand },
    )

    /**
     * Whether [card] is something a player could own as a real card outside the game: not a basic
     * land (an unlimited resource, never sideboard material — the same exclusion the Limited
     * sideboard makes), not a meld result, and castable or playable at all (a nonland printed with
     * no mana cost only enters through an alternative cost).
     */
    fun isEligible(card: CardDefinition): Boolean =
        !card.typeLine.isBasicLand && !card.meldResult && (card.isLand || !card.hasNoManaCost)

    /**
     * Builds a wishboard of up to [size] distinct cards from [candidates].
     *
     * @param candidates the cards this seat could plausibly own — the format's legal pool, the cube,
     *        or the whole corpus. Duplicate names and ineligible cards are dropped.
     * @param exclude card names that must not appear — the seat's maindeck, so a random constructed
     *        deck's wishboard is cards it *isn't* already playing.
     * @return card name → 1, in no particular order. Empty only when no candidate is eligible.
     */
    fun build(
        candidates: Collection<CardDefinition>,
        random: Random = Random.Default,
        size: Int = SIZE,
        exclude: Set<String> = emptySet(),
    ): Map<String, Int> {
        val pool = candidates
            .asSequence()
            .filter { isEligible(it) && it.name !in exclude }
            .distinctBy { it.name }
            .toMutableList()
            .apply { shuffle(random) }

        val picked = LinkedHashSet<String>()
        fun take(card: CardDefinition) {
            if (picked.size < size) picked += card.name
        }

        for (slot in SLOTS) {
            pool.asSequence()
                .filter { it.name !in picked && slot.matches(it) }
                .take(slot.count)
                .toList()
                .forEach(::take)
        }
        pool.asSequence()
            .filter { it.name !in picked }
            .take(size - picked.size)
            .toList()
            .forEach(::take)

        return picked.associateWith { 1 }
    }
}
