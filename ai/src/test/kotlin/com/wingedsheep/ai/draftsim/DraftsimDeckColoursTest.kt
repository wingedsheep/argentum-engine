package com.wingedsheep.ai.draftsim

import io.kotest.assertions.withClue
import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.shouldBe

private data class ColourCard(
    override val name: String,
    override val manaCost: String,
    override val typeLine: String,
    override val colors: List<String> = emptyList(),
    override val colorIdentity: List<String> = emptyList(),
    override val cmc: Double = DraftsimMana.cmc(manaCost),
    override val rarity: String? = "common",
) : ScorerCard

/**
 * How many colours the builder commits to, and how it splits the basics between them. Built from the
 * Reality Fracture (FRA) draft decks the AI lost to a human on 2026-11-02, where across 291 recent
 * games against people it won 37% with two colours and 33% with three.
 */
class DraftsimDeckColoursTest : FunSpec({

    class Pool {
        val ratings = HashMap<String, Double>()
        val removal = HashSet<String>()
        val archetypes = HashMap<String, DraftsimArchetypeRecord>()
        val cards = mutableListOf<DraftsimPoolCard>()

        fun add(
            name: String, cost: String, type: String, rating: Double,
            isRemoval: Boolean = false, archetype: String? = null, identity: List<String> = emptyList(),
        ) {
            val card = ColourCard(name, cost, type, DraftsimMana.colorsInCost(cost), identity.ifEmpty { DraftsimMana.colorsInCost(cost) })
            ratings[DraftsimData.nameKey(name)] = rating
            if (isRemoval) removal += name.lowercase()
            if (archetype != null) archetypes[DraftsimData.nameKey(name)] =
                DraftsimArchetypeRecord(archetypes = listOf(DraftsimArchetypeTag(archetype, "member")))
            cards += DraftsimPoolCard(card, "id-${cards.size}")
        }

        fun tables() = DraftsimSetTables(ratings, removal, archetypes)
        val byId get() = cards.associateBy { it.instanceId }
    }

    fun mainColours(build: DraftsimBuild) = build.colors.filter { it != build.splashColor }.toSet()

    // Game abb25721: FRA's archetypes are colourless themes, and "Jace Empowerment" took every colour
    // with three tagged cards in the pool — so a deep blue pool with a handful of tagged white and black
    // cards was built in all three, double-pip cards in each ({5}{W}{W}, {4}{B}{B}, {1}{U}{U}) on 7
    // Islands, 5 Swamps and 4 Plains. A named archetype is now built in its two deepest colours.
    test("a named archetype tagged across three colours builds two of them, not all three") {
        val p = Pool()
        val arch = "Jace Empowerment"
        val blue = listOf("{1}{U}", "{2}{U}", "{1}{U}", "{3}{U}", "{2}{U}", "{4}{U}", "{1}{U}", "{2}{U}", "{3}{U}", "{2}{U}")
        blue.forEachIndexed { i, cost -> p.add("Blue Scholar $i", cost, "Creature — Human Wizard", 3.0, archetype = arch) }
        p.add("Countersculpt", "{U}{U}", "Instant", 3.2, isRemoval = true, archetype = arch)
        p.add("Sphinx's Approach", "{1}{U}{U}", "Sorcery", 3.1, archetype = arch)
        p.add("Unsummon", "{U}", "Instant", 2.6, isRemoval = true)
        p.add("Kindred Judgment", "{5}{W}{W}", "Sorcery", 3.8, isRemoval = true, archetype = arch)
        p.add("Hexhaven Battalion", "{4}{W}{W}", "Sorcery", 3.5, archetype = arch)
        listOf("{1}{W}", "{2}{W}", "{3}{W}").forEachIndexed { i, cost ->
            p.add("White Adept $i", cost, "Creature — Human Cleric", 3.0, archetype = arch)
        }
        p.add("Apex Witchstalker", "{4}{B}{B}", "Creature — Horror", 3.8, archetype = arch)
        listOf("{1}{B}", "{2}{B}", "{3}{B}").forEachIndexed { i, cost ->
            p.add("Black Adept $i", cost, "Creature — Vampire Warlock", 3.0, archetype = arch)
        }
        p.add("Terminal Criticism", "{2}{B}", "Instant", 3.4, isRemoval = true, archetype = arch)
        p.add("White Soldier", "{2}{W}", "Creature — Human Soldier", 2.6)
        p.add("Black Rogue", "{2}{B}", "Creature — Human Rogue", 2.6)
        listOf("{1}{G}", "{2}{G}", "{3}{G}", "{4}{G}").forEachIndexed { i, cost -> p.add("Green Beast $i", cost, "Creature — Beast", 2.2) }
        listOf("{1}{R}", "{2}{R}", "{3}{R}", "{4}{R}").forEachIndexed { i, cost -> p.add("Red Goblin $i", cost, "Creature — Goblin", 2.0) }
        p.add("Fatehold Annex", "", "Land", 2.0, identity = listOf("W", "B"))

        // The engine AI builds every limited pool, drafted ones included, in "sealed" mode.
        val best = DraftsimDeckBuilder(p.tables()).buildDecks(p.cards, mode = "sealed").first()

        withClue("${best.name} ${best.colors} splash=${best.splashColor} basics=${best.basicsNeeded}") {
            mainColours(best).size shouldBe 2
            ("U" in mainColours(best)) shouldBe true
            // A splash, if one is taken, is light: one pip of it per card — no {4}{B}{B} off three Swamps.
            val splash = best.splashColor
            if (splash != null) best.deckInstanceIds.mapNotNull { p.byId[it]?.card }
                .forEach { card -> ((DraftsimMana.castRequirement(card.manaCost).plainPips[splash] ?: 0) <= 1) shouldBe true }
        }
    }

    // Game 081b5dc2: a blue deck (thirteen cards, Countersculpt's {U}{U} among them) with five white
    // cards and four black ones came out on 6 Islands, 7 Plains and 3 Swamps. One {4}{W}{W} card had
    // raised white to blue's floor, and the black splash's sources were then taken off the biggest
    // basic count. The splash now takes them from the colour with the most sources for its demand.
    test("a splash's sources come off the colour with the most to spare, never leaving the main colour short") {
        val p = Pool()
        val blue = listOf("{1}{U}", "{2}{U}", "{1}{U}", "{3}{U}", "{2}{U}", "{4}{U}", "{1}{U}", "{2}{U}", "{3}{U}", "{2}{U}", "{3}{U}", "{1}{U}")
        blue.forEachIndexed { i, cost -> p.add("Blue Scholar $i", cost, "Creature — Merfolk Wizard", 3.0) }
        p.add("Countersculpt", "{U}{U}", "Instant", 3.2, isRemoval = true)
        p.add("Hexhaven Battalion", "{4}{W}{W}", "Sorcery", 3.3)
        p.add("Tomik, Orzhov Lawmage", "{1}{W}", "Legendary Creature — Human Advisor", 3.0)
        p.add("Prophesied End", "{1}{W}", "Instant", 3.2, isRemoval = true)
        p.add("Koth of the Homestead", "{2}{W}", "Legendary Creature — Human Citizen", 3.0)
        p.add("White Medic", "{3}{W}", "Creature — Human Cleric", 2.8)
        p.add("Extended Absence", "{3}{B}", "Instant", 3.4, isRemoval = true)
        p.add("Null Summoner", "{2}{U}{B}", "Creature — Human Warlock", 3.2)
        p.add("Black Horror", "{4}{B}", "Creature — Horror", 3.0)
        p.add("Theoretical Necromancer", "{3}{B}", "Creature — Vampire Warlock", 2.9)
        p.add("Theorix Annex", "", "Land", 2.0, identity = listOf("U", "B"))

        val best = DraftsimDeckBuilder(p.tables()).buildDecks(p.cards, mode = "sealed").first()

        withClue("${best.name} ${best.colors} splash=${best.splashColor} basics=${best.basicsNeeded}") {
            best.splashColor shouldBe "B"
            mainColours(best) shouldBe setOf("U", "W")
            // Blue — thirteen cards, a {U}{U} two-drop — runs more basics than white's five cards.
            (best.basicsNeeded["U"] ?: 0) shouldBeGreaterThan (best.basicsNeeded["W"] ?: 0)
        }
    }
})
