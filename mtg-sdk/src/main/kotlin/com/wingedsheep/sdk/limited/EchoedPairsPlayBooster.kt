package com.wingedsheep.sdk.limited

import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import kotlin.random.Random

/**
 * Reality Fracture Play Booster: a [PlayBooster] whose three-card echoed slot always holds one
 * complete echoed pair plus one echoed card from a different pair. Paper composition:
 *
 *  - 6 commons (one may be a Special Guest — not modeled)
 *  - 1 uncommon
 *  - 1 common-or-uncommon (23% common, 77% uncommon)
 *  - 3 echoed cards: both halves of one pair + 1 echoed card not from that pair
 *  - 1 rare or mythic rare
 *  - 1 traditional-foil wildcard of any rarity (echoed cards included)
 *  - 1 land
 *
 * Echoed cards come only from the echoed slot and the wildcard; the common, uncommon, and
 * rare/mythic slots draw from the rest of the set. Per-card weights follow the paper sheets
 * (taw/magic-search-engine `fra-play.yaml`): a pair and the third echoed card are weighted 5:2:1
 * per card for uncommon:rare:mythic; the rare/mythic slot 2:1 per card for rare:mythic; the
 * wildcard 40:22:6:3 per card for common:uncommon:rare:mythic.
 *
 * Engine adaptation: foils, borderless treatments, and Special Guests are not modeled and the land
 * slot is omitted (basics are supplied at deck building), giving 13 cards per pack.
 *
 * Pairs missing a half from the pool (unimplemented or host-banned) are never opened. If no
 * complete pair remains, the echoed slot falls back to three unpaired echoed cards, then to
 * regular uncommons, commons, rares, then mythics, filling each missing echoed slot while
 * unused regular cards remain.
 *
 * @param echoedPairs Both halves of each echoed pair, by card name. Every named card is treated
 *                    as echoed and kept out of the regular slots.
 */
data class EchoedPairsPlayBooster(
    val echoedPairs: List<Pair<String, String>>,
    val commons: Int = 6,
    val uncommons: Int = 1,
    val commonOrUncommon: Int = 1,
    val commonOrUncommonCommonChance: Double = 0.23,
    val raresOrMythics: Int = 1,
    val wildcards: Int = 1,
) : BoosterStrategy {

    private val echoedNames: Set<String> = echoedPairs.flatMapTo(hashSetOf()) { listOf(it.first, it.second) }

    override fun generate(pool: List<CardDefinition>, random: Random): List<CardDefinition> {
        val (echoed, regular) = pool.partition { it.name in echoedNames }
        val regularPicker = RarityPicker(regular, random)
        val used = mutableSetOf<String>()
        val booster = mutableListOf<CardDefinition>()
        fun MutableList<CardDefinition>.addPick(card: CardDefinition?) {
            card ?: return
            add(card)
            used.add(card.name)
        }

        repeat(commons) { booster.addPick(regularPicker.pick(Rarity.COMMON) ?: regularPicker.pick(Rarity.UNCOMMON)) }
        repeat(uncommons) { booster.addPick(regularPicker.pick(Rarity.UNCOMMON) ?: regularPicker.pick(Rarity.COMMON)) }
        repeat(commonOrUncommon) {
            val first = if (random.nextDouble() < commonOrUncommonCommonChance) Rarity.COMMON else Rarity.UNCOMMON
            booster.addPick(regularPicker.pick(first) ?: regularPicker.pick(Rarity.COMMON) ?: regularPicker.pick(Rarity.UNCOMMON))
        }

        val echoedSlot = pickEchoedSlot(echoed, random)
        echoedSlot.forEach { booster.addPick(it) }
        repeat(ECHOED_SLOT_SIZE - echoedSlot.size) {
            booster.addPick(
                regularPicker.pick(Rarity.UNCOMMON)
                    ?: regularPicker.pick(Rarity.COMMON)
                    ?: regularPicker.pick(Rarity.RARE)
                    ?: regularPicker.pick(Rarity.MYTHIC)
            )
        }

        repeat(raresOrMythics) {
            val card = weightedPick(regular.filter { it.name !in used }, random, ::rareSlotWeight)
                ?: regularPicker.pick(Rarity.UNCOMMON)
                ?: regularPicker.pick(Rarity.COMMON)
                ?: throw IllegalStateException("No cards available for booster generation")
            regularPicker.markUsed(card.name)
            booster.addPick(card)
        }

        repeat(wildcards) {
            val card = weightedPick(pool.filter { it.name !in used }, random, ::wildcardWeight)
            card?.let { regularPicker.markUsed(it.name) }
            booster.addPick(card)
        }
        return booster
    }

    /** One complete pair plus an echoed card from another pair; best-effort when pairs are missing. */
    private fun pickEchoedSlot(echoed: List<CardDefinition>, random: Random): List<CardDefinition> {
        val byName = echoed.associateBy { it.name }
        val completePairs = echoedPairs.mapNotNull { (a, b) ->
            val first = byName[a] ?: return@mapNotNull null
            val second = byName[b] ?: return@mapNotNull null
            first to second
        }
        val pair = weightedPick(completePairs, random) { echoedWeight(it.first) }
        val slot = pair?.toList().orEmpty().toMutableList()
        while (slot.size < ECHOED_SLOT_SIZE) {
            val taken = slot.mapTo(hashSetOf()) { it.name }
            val next = weightedPick(echoed.filter { it.name !in taken }, random, ::echoedWeight) ?: break
            slot.add(next)
        }
        return slot
    }

    private fun echoedWeight(card: CardDefinition): Int = when (card.metadata.rarity) {
        Rarity.MYTHIC -> 1
        Rarity.RARE -> 2
        else -> 5
    }

    private fun rareSlotWeight(card: CardDefinition): Int = when (card.metadata.rarity) {
        Rarity.MYTHIC -> 1
        Rarity.RARE -> 2
        else -> 0
    }

    private fun wildcardWeight(card: CardDefinition): Int = when (card.metadata.rarity) {
        Rarity.MYTHIC -> 3
        Rarity.RARE -> 6
        Rarity.UNCOMMON -> 22
        else -> 40
    }

    private companion object {
        const val ECHOED_SLOT_SIZE = 3
    }
}
