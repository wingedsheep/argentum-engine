package com.wingedsheep.ai.engine.deck

import com.wingedsheep.sdk.core.Color

/**
 * Archetype definition for a set's draft/deck strategy.
 */
data class Archetype(
    val name: String,
    val colors: List<Color>,
    val description: String,
    val creatureTypes: List<String> = emptyList()
)

/**
 * Set-level synergy and archetype data.
 */
data class SetSynergies(
    val setCode: String,
    val setName: String,
    val archetypes: List<Archetype>
)

/**
 * Static archetype data for all supported sets.
 * This is the single source of truth — both the frontend API endpoint and AI deckbuilder use it.
 */
object SetArchetypes {

    private val allSets: Map<String, SetSynergies> = mapOf(
        "POR" to SetSynergies(
            setCode = "POR",
            setName = "Portal",
            archetypes = listOf(
                Archetype("White Weenie", listOf(Color.WHITE),
                    "Small efficient creatures with combat bonuses. Go wide with cheap soldiers and pump effects to overwhelm before your opponent stabilizes."),
                Archetype("Blue Flyers", listOf(Color.BLUE),
                    "Evasive flying creatures backed by card draw sorceries. A tempo-oriented strategy that wins in the air."),
                Archetype("Black Control", listOf(Color.BLACK),
                    "Creature removal sorceries and drain effects paired with midrange creatures. Grind opponents out with efficient answers."),
                Archetype("Red Aggro", listOf(Color.RED),
                    "Aggressive cheap creatures and direct damage sorceries to burn the opponent out before they can set up defenses."),
                Archetype("Green Stompy", listOf(Color.GREEN),
                    "Large efficient creatures that overpower opponents through raw stats and size. Simple but effective."),
            )
        ),
        "ONS" to SetSynergies(
            setCode = "ONS",
            setName = "Onslaught",
            archetypes = listOf(
                Archetype("Wizards", listOf(Color.BLUE, Color.RED),
                    "Attach auras to Wizards for repeatable removal, and use shapeshifters for extra tribal synergy. A spell-based control deck.",
                    creatureTypes = listOf("Wizard")),
                Archetype("Clerics", listOf(Color.WHITE, Color.BLACK),
                    "White Clerics defend and heal while black Clerics drain life. The tribe rewards a slow, grinding game plan built on incremental advantages.",
                    creatureTypes = listOf("Cleric")),
                Archetype("Zombies", listOf(Color.BLACK),
                    "Recursive threats and strong removal fuel an attrition-based strategy that grinds opponents down relentlessly.",
                    creatureTypes = listOf("Zombie")),
                Archetype("Goblins", listOf(Color.BLACK, Color.RED),
                    "Red aggression paired with black removal. Goblin tribal synergies create explosive starts backed by efficient creature destruction.",
                    creatureTypes = listOf("Goblin")),
                Archetype("Elves", listOf(Color.GREEN),
                    "Elves generate mana, gain life, and pump each other. The more Elves you draft, the stronger every individual Elf becomes.",
                    creatureTypes = listOf("Elf")),
                Archetype("Beasts", listOf(Color.RED, Color.GREEN),
                    "Green's large beasts combined with red removal create a midrange deck that wins through creature quality and raw combat dominance.",
                    creatureTypes = listOf("Beast")),
                Archetype("Soldiers / Flyers", listOf(Color.WHITE, Color.BLUE),
                    "Cheap evasive flyers and Soldier tribal put opponents on a fast clock. A tempo-aggro deck that races on the ground and in the air.",
                    creatureTypes = listOf("Soldier", "Bird")),
                Archetype("Morph", listOf(Color.BLUE, Color.GREEN),
                    "Face-down creatures conceal deadly surprises. Keep your opponent guessing while you set up devastating flip triggers and tempo plays."),
                Archetype("Cycling", listOf(Color.WHITE, Color.RED),
                    "Cycle through your deck to find answers at the right time. Cycling triggers and payoffs generate incremental value while smoothing draws."),
            )
        ),
        "LGN" to SetSynergies(
            setCode = "LGN",
            setName = "Legions",
            archetypes = listOf(
                Archetype("Soldiers / Flyers", listOf(Color.WHITE, Color.BLUE),
                    "Soldiers and Birds combine for a tempo-evasion strategy. Fly over stalled boards while tribal lords pump your ground forces.",
                    creatureTypes = listOf("Soldier", "Bird")),
                Archetype("Clerics", listOf(Color.WHITE, Color.BLACK),
                    "The cleric synergy engine continues. In an all-creature set with no removal spells, the lifegain/drain plan is even more dominant.",
                    creatureTypes = listOf("Cleric")),
                Archetype("Goblins", listOf(Color.BLACK, Color.RED),
                    "Morph Goblins provide rare removal in a removal-starved set. Amplify lords reward drafting Goblins aggressively for explosive starts.",
                    creatureTypes = listOf("Goblin")),
                Archetype("Elves", listOf(Color.GREEN, Color.WHITE),
                    "Elf lords that tap to pump make every Elf a threat. Amplify and provoke create an engine of growth that overwhelms with sheer numbers.",
                    creatureTypes = listOf("Elf")),
                Archetype("Zombies", listOf(Color.BLUE, Color.BLACK),
                    "Zombie tribal mixed with blue evasion. Morph creatures provide tempo plays with face-down threats that flip into value.",
                    creatureTypes = listOf("Zombie")),
                Archetype("Slivers", listOf(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN),
                    "Every Sliver shares its abilities with all others. A risky but powerful tribal strategy — if the pieces come together, the hive is unstoppable.",
                    creatureTypes = listOf("Sliver")),
                Archetype("Morph", listOf(Color.BLUE, Color.GREEN),
                    "An all-creature set means face-down threats are everywhere. Bluff and outmaneuver your opponent with morph mind games on every turn."),
            )
        ),
        "SCG" to SetSynergies(
            setCode = "SCG",
            setName = "Scourge",
            archetypes = listOf(
                Archetype("Goblins", listOf(Color.BLACK, Color.RED),
                    "Black removal plus Goblin tribal synergies. Storm spells can steal games out of nowhere.",
                    creatureTypes = listOf("Goblin")),
                Archetype("Graveyard Value", listOf(Color.BLACK, Color.GREEN),
                    "Use cycling and self-mill to fill the graveyard, then recur creatures en masse. Landcycling fixes mana while providing late-game bodies."),
                Archetype("Clerics / Control", listOf(Color.WHITE, Color.BLACK),
                    "White landcyclers ensure land drops while black provides efficient removal. A solid control strategy.",
                    creatureTypes = listOf("Cleric")),
                Archetype("Wizards", listOf(Color.BLUE, Color.RED),
                    "Draw cards proportional to your highest mana cost for massive card advantage. Wizard synergies from the block still anchor the deck.",
                    creatureTypes = listOf("Wizard")),
                Archetype("Ramp", listOf(Color.BLUE, Color.GREEN),
                    "Ramp into large green creatures, then leverage their high mana cost for powerful draw spells. Bury opponents in card advantage."),
                Archetype("Dragons", listOf(Color.RED),
                    "Dedicated Dragon tribal support rewards these devastating flying threats. High mana costs are a feature, not a bug.",
                    creatureTypes = listOf("Dragon")),
                Archetype("Landcycling", listOf(Color.WHITE, Color.GREEN),
                    "Landcycling creatures fix your mana early and provide large bodies late. Smooths draws across multiple colors."),
            )
        ),
        "DOM" to SetSynergies(
            setCode = "DOM",
            setName = "Dominaria",
            archetypes = listOf(
                Archetype("Historic Fliers", listOf(Color.WHITE, Color.BLUE),
                    "Evasive flying creatures backed by historic synergies. Artifacts, legendaries, and Sagas trigger payoffs while your flyers close the game in the air."),
                Archetype("Historic Control", listOf(Color.BLUE, Color.BLACK),
                    "Grind opponents out with Sagas, removal, and card advantage. Sagas schedule value over three turns while efficient answers keep the board in check."),
                Archetype("Reckless Aggro", listOf(Color.BLACK, Color.RED),
                    "Fast creatures with first strike and haste backed by sacrifice synergies. Tokens serve as expendable resources while you race to close the game."),
                Archetype("Kicker Ramp", listOf(Color.RED, Color.GREEN),
                    "Ramp into large creatures and leverage kicker for late-game flexibility. Cards scale from on-curve plays to devastating threats when you have extra mana."),
                Archetype("Tokens", listOf(Color.GREEN, Color.WHITE),
                    "Flood the board with Saproling and Knight tokens, then use anthems and equipment to turn your wide board into lethal damage."),
                Archetype("Legendary Creatures", listOf(Color.WHITE, Color.BLACK),
                    "Individually powerful legendary creatures backed by premium removal. A minor Knight tribal subtheme adds synergy to this value-oriented strategy.",
                    creatureTypes = listOf("Knight")),
                Archetype("Wizard Spells", listOf(Color.BLUE, Color.RED),
                    "Wizards grow stronger with every instant and sorcery you cast. Build a critical mass of spells and Wizards for explosive prowess-style turns.",
                    creatureTypes = listOf("Wizard")),
                Archetype("Saproling Sacrifice", listOf(Color.BLACK, Color.GREEN),
                    "Generate Saprolings and sacrifice them for value. Thallids and fungus creatures create a self-sustaining engine of tokens and drain effects."),
                Archetype("Auras & Equipment", listOf(Color.RED, Color.WHITE),
                    "Cheap creatures augmented by Auras and Equipment for an aggressive voltron strategy. Tiana recovers lost Auras to mitigate card disadvantage."),
                Archetype("Ramp", listOf(Color.GREEN, Color.BLUE),
                    "Green mana acceleration paired with blue card draw and kicker payoffs. Reach expensive threats and draw extra cards off land drops with Tatyova."),
            )
        ),
        "KTK" to SetSynergies(
            setCode = "KTK",
            setName = "Khans of Tarkir",
            archetypes = listOf(
                Archetype("Abzan", listOf(Color.WHITE, Color.BLACK, Color.GREEN),
                    "Outlast your creatures to grow +1/+1 counters, then share keywords like flying and lifelink across your bolstered army. A grindy, resilient midrange strategy."),
                Archetype("Jeskai", listOf(Color.BLUE, Color.RED, Color.WHITE),
                    "Cast noncreature spells to trigger prowess, turning every instant and sorcery into a combat trick. Combines card selection, burn, and tempo."),
                Archetype("Sultai", listOf(Color.BLACK, Color.GREEN, Color.BLUE),
                    "Fill your graveyard to fuel powerful delve spells at reduced cost. A midrange-to-control strategy built on exploiting the graveyard as a resource."),
                Archetype("Mardu", listOf(Color.RED, Color.WHITE, Color.BLACK),
                    "Attack each turn to trigger raid bonuses. Wide board presence with tokens and warriors, backed by removal to clear blockers."),
                Archetype("Temur", listOf(Color.GREEN, Color.BLUE, Color.RED),
                    "Ferocious abilities activate when you control a creature with power 4 or greater. Ramp into the biggest threats in the format."),
                Archetype("Warriors", listOf(Color.WHITE, Color.BLACK),
                    "Flood the board with cheap Warriors and pump them with tribal lords for an aggressive, low-curve strategy.",
                    creatureTypes = listOf("Warrior")),
                Archetype("Morph", listOf(Color.BLUE, Color.GREEN),
                    "Build around face-down creatures for card advantage and tempo. Morph rewards ramping to flip costs first and keeping opponents guessing."),
                Archetype("Prowess / Spells", listOf(Color.BLUE, Color.RED),
                    "Spell-heavy tempo deck. Noncreature spells trigger prowess on your creatures and activate enchantments that tax or generate tokens."),
                Archetype("Toughness Matters", listOf(Color.BLACK, Color.GREEN),
                    "Draft high-toughness creatures and defenders, then convert that toughness into offense with spells that create tokens based on toughness."),
                Archetype("Token Aggro", listOf(Color.RED, Color.WHITE),
                    "Generate tokens with creature and spell makers, then use mass pump effects to turn a wide board into lethal damage."),
            )
        ),
    )

    /** Get archetypes for a specific set code, or null if not found. */
    fun getForSet(setCode: String): SetSynergies? = allSets[setCode.uppercase()]

    /** Get all available set synergies. */
    fun getAll(): Map<String, SetSynergies> = allSets

    /** Get archetypes matching the given colors for a set. */
    fun getMatchingArchetypes(setCode: String, colors: Set<Color>): List<Archetype> {
        val synergies = allSets[setCode.uppercase()] ?: return emptyList()
        return synergies.archetypes.filter { archetype ->
            archetype.colors.toSet() == colors || archetype.colors.all { it in colors }
        }
    }
}
