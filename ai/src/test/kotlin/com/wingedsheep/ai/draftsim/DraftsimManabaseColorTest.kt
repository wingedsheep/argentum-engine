package com.wingedsheep.ai.draftsim

import io.kotest.core.spec.style.FunSpec
import io.kotest.matchers.shouldBe

private data class RCard(
    override val name: String,
    override val manaCost: String,
    override val typeLine: String,
    override val colors: List<String> = emptyList(),
    override val colorIdentity: List<String> = emptyList(),
    override val cmc: Double = DraftsimMana.cmc(manaCost),
    override val rarity: String? = "common",
) : ScorerCard

class DraftsimManabaseColorTest : FunSpec({
    fun buildPool(): List<DraftsimPoolCard> {
        val cards = mutableListOf<DraftsimPoolCard>()
        var n = 0
        fun add(card: ScorerCard) { cards += DraftsimPoolCard(card, "id-${n++}") }
        fun BCard(name: String, cost: String, type: String, colors: List<String> = emptyList(), ci: List<String> = emptyList()) =
            RCard(name, cost, type, colors, ci)
                add(BCard("Agent Bishop, Man in Black", "{2}{W}", "Legendary Creature — Human Soldier", listOf("W"), listOf("W")))
                add(BCard("Bot Bashing Time", "{3}{R}", "Sorcery", listOf("R"), listOf("R")))
                add(BCard("Brilliance Unleashed", "{4}{U}{R}", "Sorcery", listOf("R", "U"), listOf("R", "U")))
                add(BCard("Donatello, Gadget Master", "{2}{U}", "Legendary Creature — Mutant Ninja Turtle", listOf("U"), listOf("U")))
                add(BCard("Frog Butler", "{1}{G}", "Creature — Frog Spirit", listOf("G"), listOf("G")))
                add(BCard("Frog Butler", "{1}{G}", "Creature — Frog Spirit", listOf("G"), listOf("G")))
                add(BCard("General Traag, Heart of Stone", "{3}{R}{R}", "Legendary Artifact Creature — Elemental Soldier", listOf("R"), listOf("R")))
                add(BCard("Hard-Won Jitte", "{1}{R}", "Artifact — Equipment", listOf("R"), listOf("R")))
                add(BCard("Improvised Arsenal", "{1}{R}", "Artifact — Equipment", listOf("R"), listOf("R")))
                add(BCard("Jennika's Technique", "{2}{R}", "Instant", listOf("R"), listOf("R")))
                add(BCard("Krang, Master Mind", "{6}{U}{U}", "Legendary Artifact Creature — Utrom Warrior", listOf("U"), listOf("U")))
                add(BCard("Mind Transfer Protocol", "{2}{U}", "Instant", listOf("U"), listOf("U")))
                add(BCard("Mind Transfer Protocol", "{2}{U}", "Instant", listOf("U"), listOf("U")))
                add(BCard("Mouser Mark III", "{1}{U/R}", "Artifact Creature — Robot", listOf("R", "U"), listOf("R", "U")))
                add(BCard("Mouser Mark III", "{1}{U/R}", "Artifact Creature — Robot", listOf("R", "U"), listOf("R", "U")))
                add(BCard("Nobody", "{1}{U/R}{U/R}", "Artifact Creature — Human Hero", listOf("R", "U"), listOf("R", "U")))
                add(BCard("Raphael, Tough Turtle", "{1}{R}", "Legendary Creature — Mutant Ninja Turtle", listOf("R"), listOf("R")))
                add(BCard("Ray Fillet, Man Ray", "{3}{U}", "Legendary Creature — Fish Mutant", listOf("U"), listOf("U")))
                add(BCard("Return to the Sewers", "{3}{U}", "Instant", listOf("U"), listOf("U")))
                add(BCard("Rock Soldiers", "{3}{R}", "Artifact Creature — Elemental Soldier", listOf("R"), listOf("R")))
                add(BCard("TCRI Building", "", "Land", emptyList(), listOf("R", "U")))
                add(BCard("Turtle Blimp", "{5}", "Artifact — Vehicle", emptyList(), emptyList()))
                add(BCard("Turtle Blimp", "{5}", "Artifact — Vehicle", emptyList(), emptyList()))
                add(BCard("Utrom Scientists", "{2}{U}", "Artifact Creature — Utrom Robot Scientist", listOf("U"), listOf("U")))
                add(BCard("Action News Crew", "{1}{W}", "Creature — Human Citizen", listOf("W"), listOf("W")))
                add(BCard("Bebop, Warthog Warrior", "{4}{B}", "Legendary Creature — Boar Mutant Warrior", listOf("B"), listOf("B")))
                add(BCard("Cowabunga!", "{G}", "Sorcery", listOf("G"), listOf("G")))
                add(BCard("Cowabunga!", "{G}", "Sorcery", listOf("G"), listOf("G")))
                add(BCard("Dimension X", "", "Land", emptyList(), listOf("R", "W")))
                add(BCard("Dimensional Exile", "{1}{W}", "Enchantment — Aura", listOf("W"), listOf("W")))
                add(BCard("East Wind Avatar", "{3}{W}", "Creature — Bird Spirit Avatar", listOf("W"), listOf("W")))
                add(BCard("Foot Mystic", "{3}{B}", "Creature — Human Ninja Warlock", listOf("B"), listOf("B")))
                add(BCard("Foot Mystic", "{3}{B}", "Creature — Human Ninja Warlock", listOf("B"), listOf("B")))
                add(BCard("Foot Ninjas", "{4}{W/B}{W/B}", "Creature — Human Ninja", listOf("B", "W"), listOf("B", "W")))
                add(BCard("Foot Ninjas", "{4}{W/B}{W/B}", "Creature — Human Ninja", listOf("B", "W"), listOf("B", "W")))
                add(BCard("General Traag, Heart of Stone", "{3}{R}{R}", "Legendary Artifact Creature — Elemental Soldier", listOf("R"), listOf("R")))
                add(BCard("Groundchuck & Dirtbag", "{4}{G}{G}", "Legendary Creature — Ox Mole Mutant", listOf("G"), listOf("G")))
                add(BCard("Groundchuck & Dirtbag", "{4}{G}{G}", "Legendary Creature — Ox Mole Mutant", listOf("G"), listOf("G")))
                add(BCard("Guac & Marshmallow Pizza", "{G}", "Artifact — Food", listOf("G"), listOf("G")))
                add(BCard("Guac & Marshmallow Pizza", "{G}", "Artifact — Food", listOf("G"), listOf("G")))
                add(BCard("Hamato Guardian Stance", "{W}", "Instant", listOf("W"), listOf("W")))
                add(BCard("Hamato Guardian Stance", "{W}", "Instant", listOf("W"), listOf("W")))
                add(BCard("Hard-Won Jitte", "{1}{R}", "Artifact — Equipment", listOf("R"), listOf("R")))
                add(BCard("Hard-Won Jitte", "{1}{R}", "Artifact — Equipment", listOf("R"), listOf("R")))
                add(BCard("High-Flying Ace", "{2}{W}", "Creature — Bird Mutant", listOf("W"), listOf("W")))
                add(BCard("High-Flying Ace", "{2}{W}", "Creature — Bird Mutant", listOf("W"), listOf("W")))
                add(BCard("Make Your Move", "{2}{W}", "Instant", listOf("W"), listOf("W")))
                add(BCard("Mikey & Leo, Chaos & Order", "{G/W}{G/W}", "Legendary Creature — Mutant Ninja Turtle", listOf("G", "W"), listOf("G", "W")))
                add(BCard("Mutagen Man, Living Ooze", "{X}{G}{G}", "Legendary Creature — Ooze Mutant", listOf("G"), listOf("G")))
                add(BCard("Mutant Chain Reaction", "{2}{G}", "Sorcery", listOf("G"), listOf("G")))
                add(BCard("New Generation's Technique", "{3}{G}", "Sorcery", listOf("G"), listOf("G")))
                add(BCard("Null Group Biological Assets", "{2}{R}", "Creature — Mutant Mercenary", listOf("R"), listOf("R")))
                add(BCard("Null Group Biological Assets", "{2}{R}", "Creature — Mutant Mercenary", listOf("R"), listOf("R")))
                add(BCard("Oroku Saki, Shredder Rising", "{2}{B}", "Legendary Creature — Human Ninja", listOf("B"), listOf("B")))
                add(BCard("Oroku Saki, Shredder Rising", "{2}{B}", "Legendary Creature — Human Ninja", listOf("B"), listOf("B")))
                add(BCard("Pain 101", "{1}{B}", "Instant", listOf("B"), listOf("B")))
                add(BCard("Pain 101", "{1}{B}", "Instant", listOf("B"), listOf("B")))
                add(BCard("Pain 101", "{1}{B}", "Instant", listOf("B"), listOf("B")))
                add(BCard("Paramecia Coloniex", "{1}{B}", "Creature — Zombie Worm", listOf("B"), listOf("B")))
                add(BCard("Paramecia Coloniex", "{1}{B}", "Creature — Zombie Worm", listOf("B"), listOf("B")))
                add(BCard("Paramecia Coloniex", "{1}{B}", "Creature — Zombie Worm", listOf("B"), listOf("B")))
                add(BCard("Pizza Face, Gastromancer", "{3}{B}{G}", "Legendary Artifact Creature — Food Mutant", listOf("B", "G"), listOf("B", "G")))
                add(BCard("Shredder's Revenge", "{2}{B}", "Sorcery", listOf("B"), listOf("B")))
                add(BCard("Shredder's Revenge", "{2}{B}", "Sorcery", listOf("B"), listOf("B")))
                add(BCard("Shredder's Technique", "{2}{B}", "Sorcery", listOf("B"), listOf("B")))
                add(BCard("Skateboard", "{1}", "Artifact — Equipment", emptyList(), emptyList()))
                add(BCard("Slithering Cryptid", "{2}{G/U}", "Creature — Fish Mutant", listOf("G", "U"), listOf("G", "U")))
                add(BCard("Slithering Cryptid", "{2}{G/U}", "Creature — Fish Mutant", listOf("G", "U"), listOf("G", "U")))
                add(BCard("South Wind Avatar", "{3}{B}", "Creature — Snake Spirit Avatar", listOf("B"), listOf("B")))
                add(BCard("Squirrelanoids", "{B}", "Creature — Squirrel Mutant", listOf("B"), listOf("B")))
                add(BCard("Stockman, Mad Fly-entist", "{4}{U}", "Legendary Creature — Insect Mutant Scientist", listOf("U"), listOf("U")))
                add(BCard("Stockman, Mad Fly-entist", "{4}{U}", "Legendary Creature — Insect Mutant Scientist", listOf("U"), listOf("U")))
                add(BCard("Stomped by the Foot", "{1}{B}", "Instant", listOf("B"), listOf("B")))
                add(BCard("Tenderize", "{1}{G}", "Instant", listOf("G"), listOf("G")))
                add(BCard("Turtle Lair", "", "Land", emptyList(), emptyList()))
                add(BCard("Turtle Lair", "", "Land", emptyList(), emptyList()))
                add(BCard("Uneasy Alliance", "{1}{W}", "Enchantment — Aura", listOf("W"), listOf("W")))
                add(BCard("Zog, Triceraton Castaway", "{4}{R}", "Legendary Creature — Dinosaur Soldier", listOf("R"), listOf("R")))
        return cards
    }

    // §3.4 invariant: the manabase only fixes a build's reported colors. A castability-relaxed fill can
    // pull an off-color card into the deck, but it must never make the builder fabricate that color's
    // basics — the deck the player sees would then carry, e.g., Plains under an "RU" header. Regression
    // for the real TMT sealed pool that produced colors=[R,U] but basicsNeeded={R,U,G,W}.
    test("fresh auto-build: every basic fixes a reported build color (no off-color manabase leak)") {
        val builder = DraftsimDeckBuilder(DraftsimData.tablesFor(listOf("TMT")))
        val builds = builder.buildDecks(buildPool(), mode = "sealed")
        builds.isNotEmpty() shouldBe true
        for (b in builds) b.basicsNeeded.keys shouldBe b.basicsNeeded.keys.filter { it in b.colors }.toSet()
    }

    test("completion: keeping the 4-color maindeck commits to 4 colors AND fixes all of them") {
        val builder = DraftsimDeckBuilder(DraftsimData.tablesFor(listOf("TMT")))
        val pool = buildPool()
        val mainNames = listOf(
            "Agent Bishop, Man in Black", "Bot Bashing Time", "Brilliance Unleashed", "Donatello, Gadget Master",
            "Frog Butler", "Frog Butler", "General Traag, Heart of Stone", "Hard-Won Jitte", "Improvised Arsenal",
            "Jennika's Technique", "Krang, Master Mind", "Mind Transfer Protocol", "Mind Transfer Protocol",
            "Mouser Mark III", "Mouser Mark III", "Nobody", "Raphael, Tough Turtle", "Ray Fillet, Man Ray",
            "Return to the Sewers", "Rock Soldiers", "Turtle Blimp", "Turtle Blimp", "Utrom Scientists",
        )
        val remaining = mainNames.groupingBy { it }.eachCount().toMutableMap()
        val forced = HashSet<String>()
        for (pc in pool) {
            val c = remaining[pc.card.name] ?: continue
            if (c <= 0 || pc.card.typeLine.contains("Land", true)) continue
            forced += pc.instanceId; remaining[pc.card.name] = c - 1
        }
        val build = builder.buildDecks(pool, mode = "sealed", forced = forced).single()
        // The locked cards span W/U/B/R/G — completion commits to them and the label matches the manabase.
        build.colors.toSet() shouldBe setOf("R", "U", "G", "W")
        build.basicsNeeded.keys shouldBe setOf("R", "U", "G", "W")
    }

    // 2026-11-01: a near mono-white BLB deck whose only red cards were Mabel ({1}{R}{W}) and Agate
    // Assault ({2}{R}) — its R/W hybrids cast off Plains — ran six Mountains. Two one-pip cards are a
    // splash: four sources, the rest Plains.
    test("a colour only two cards strictly need gets a splash's sources, not a main colour's") {
        var n = 0
        fun pc(name: String, cost: String, type: String, colors: List<String>) =
            DraftsimPoolCard(RCard(name, cost, type, colors, colors), "light-${n++}")
        val pool = (1..19).map { pc("White $it", "{${it % 4}}{W}", "Creature — Soldier", listOf("W")) } +
            pc("Mabel", "{1}{R}{W}", "Legendary Creature — Mouse Soldier", listOf("R", "W")) +
            pc("Agate Assault", "{2}{R}", "Sorcery", listOf("R")) +
            pc("Guardmouse A", "{3}{R/W}", "Creature — Mouse Soldier", listOf("R", "W")) +
            pc("Guardmouse B", "{3}{R/W}", "Creature — Mouse Soldier", listOf("R", "W"))
        val build = DraftsimDeckBuilder(DraftsimData.tablesFor(emptyList()))
            .buildDecks(pool, mode = "sealed", forced = pool.map { it.instanceId }.toSet()).single()

        val mountains = build.basicsNeeded["R"] ?: 0
        (mountains in 3..4) shouldBe true
        build.basicsNeeded.values.sum() shouldBe 17
    }
})
