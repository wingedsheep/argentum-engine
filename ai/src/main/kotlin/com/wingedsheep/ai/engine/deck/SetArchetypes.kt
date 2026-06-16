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
        "TDM" to SetSynergies(
            setCode = "TDM",
            setName = "Tarkir: Dragonstorm",
            archetypes = listOf(
                Archetype("Abzan", listOf(Color.WHITE, Color.BLACK, Color.GREEN),
                    "Endure pads your board with +1/+1 counters or Spirit tokens. A resilient counters-matter midrange that grows wider and taller every turn and refuses to stay down."),
                Archetype("Jeskai", listOf(Color.BLUE, Color.RED, Color.WHITE),
                    "Cast your second spell each turn to trigger flurry, turning a steady stream of cheap instants and sorceries into damage, tokens, and card advantage. A snowballing tempo deck."),
                Archetype("Sultai", listOf(Color.BLACK, Color.GREEN, Color.BLUE),
                    "Fill your graveyard, then renew creatures from it to deal out +1/+1 counters and refill your hand. A grindy graveyard-value midrange that wins the long game."),
                Archetype("Mardu", listOf(Color.RED, Color.WHITE, Color.BLACK),
                    "Mobilize creates temporary Warrior tokens every time you attack. Go wide, swing hard, and back the assault with removal to push through the last points of damage.",
                    creatureTypes = listOf("Warrior")),
                Archetype("Temur", listOf(Color.GREEN, Color.BLUE, Color.RED),
                    "Behold a Dragon to unlock discounts and bonuses, then ramp into the format's biggest fliers. A ferocious ramp-and-Dragons strategy.",
                    creatureTypes = listOf("Dragon")),
                Archetype("Warriors", listOf(Color.WHITE, Color.BLACK),
                    "Aggressive Warriors backed by removal and lifedrain. Mobilize tokens and endure counters keep your board refilling faster than opponents can answer it.",
                    creatureTypes = listOf("Warrior")),
                Archetype("Token Aggro", listOf(Color.RED, Color.WHITE),
                    "Generate tokens with mobilize and other attack triggers, then turn the swarm lethal with anthems and mass pump. A fast, go-wide aggro deck."),
                Archetype("Flurry Spells", listOf(Color.BLUE, Color.RED),
                    "A spell-velocity tempo deck. A high density of cheap instants and sorceries triggers flurry payoffs for burst damage and explosive turns."),
                Archetype("Behold Ramp", listOf(Color.GREEN, Color.BLUE),
                    "Ramp and fix into expensive haymakers while beholding Dragons to power up your payoffs. A controlling midrange that out-values the table.",
                    creatureTypes = listOf("Dragon")),
                Archetype("Graveyard Counters", listOf(Color.BLACK, Color.GREEN),
                    "Self-mill and renew to recur threats from the graveyard, then pile on +1/+1 counters with endure. A grindy value deck that goes long."),
            )
        ),
        "EOE" to SetSynergies(
            setCode = "EOE",
            setName = "Edge of Eternities",
            archetypes = listOf(
                Archetype("Second Spell", listOf(Color.WHITE, Color.BLUE),
                    "Cast two spells in a single turn to fire off a stream of payoffs. Cheap interaction and artifacts keep the spells flowing while flyers and tokens reward every double-spell turn. A consistent tempo-value deck."),
                Archetype("Sacrifice", listOf(Color.WHITE, Color.BLACK),
                    "Field a wide board of efficient creatures and feed them to sacrifice payoffs. Warp lets your threats land early and return later, fueling drain, removal, and relentless pressure."),
                Archetype("Spacecraft", listOf(Color.WHITE, Color.RED),
                    "Tap your creatures to station up Spacecraft into enormous artifact threats, then back the assault with aggressive bodies. Go fast on the ground while your ships charge toward liftoff.",
                    creatureTypes = listOf("Spacecraft")),
                Archetype("Artifact Value", listOf(Color.BLUE, Color.BLACK),
                    "An artifact 'good stuff' value deck. Stock up on two-for-ones and removal, trigger void as permanents leave, and grind the game out before pulling ahead on cards."),
                Archetype("Artifact Aggro", listOf(Color.BLUE, Color.RED),
                    "Aggressively deploy artifacts to snowball +1/+1 counters and station triggers. Each artifact entering powers up your team for a fast, synergy-driven beatdown."),
                Archetype("Lander Ramp", listOf(Color.BLUE, Color.GREEN),
                    "Crack Lander tokens for mana and fixing, ramping into the format's biggest threats. The premier two-for-one ramp deck that out-resources the table and splashes bombs."),
                Archetype("Void", listOf(Color.BLACK, Color.RED),
                    "Sacrifice your own creatures to switch on void payoffs, turning every permanent that dies into value. A grindy sacrifice-aggro deck that controls the board while bleeding the opponent."),
                Archetype("Graveyard Midrange", listOf(Color.BLACK, Color.GREEN),
                    "Fill your graveyard and bring key creatures back, leaning on void and sacrifice for incidental value. A resilient midrange that trades all day and rebuilds from the yard."),
                Archetype("Landfall", listOf(Color.RED, Color.GREEN),
                    "Lands entering trigger landfall payoffs, and Landers let you set them off on demand. Ramp, sacrifice your Landers, and crash in with an ever-growing board.",
                    creatureTypes = listOf("Lander")),
                Archetype("Counters", listOf(Color.GREEN, Color.WHITE),
                    "Take the board early with cheap creatures and pile on +1/+1 counters. Station synergies and counter payoffs build a sticky, go-tall-and-wide deck that's hard to climb back against."),
            )
        ),
        "OTJ" to SetSynergies(
            setCode = "OTJ",
            setName = "Outlaws of Thunder Junction",
            archetypes = listOf(
                Archetype("Flash Plot", listOf(Color.WHITE, Color.BLUE),
                    "Plot cards on your turn, then deploy flash creatures and tricks on the opponent's. A tempo deck that always keeps mana up and threatens free spells from exile."),
                Archetype("Outlaw Attrition", listOf(Color.WHITE, Color.BLACK),
                    "Grind the game out with efficient outlaws, premium removal, and incidental lifegain. A controlling midrange that trades resources and pulls ahead on value."),
                Archetype("Mercenaries", listOf(Color.RED, Color.WHITE),
                    "Go wide with Mercenary tokens and aggressive bodies, then convert the swarm to damage with go-wide payoffs and mass pump. A fast, token-fueled aggro deck.",
                    creatureTypes = listOf("Mercenary")),
                Archetype("Crime", listOf(Color.BLUE, Color.BLACK),
                    "Commit crimes — target opponents and their stuff — to snowball value and disruption. A tempo-control deck that drains the opponent while answering their threats."),
                Archetype("Spells Plot", listOf(Color.BLUE, Color.RED),
                    "Cast a high volume of cheap instants and sorceries, plotting spells to fire off multiple per turn. Spell-velocity payoffs reward explosive double-spell turns."),
                Archetype("Ramp Plot", listOf(Color.BLUE, Color.GREEN),
                    "Ramp and discount plot cards to set up powerful future turns, then land the format's biggest threats ahead of schedule. A controlling midrange that out-values the table."),
                Archetype("Outlaws", listOf(Color.BLACK, Color.RED),
                    "The dedicated outlaw deck — Assassins, Mercenaries, Pirates, Rogues, and Warlocks trigger aggressive payoffs. Apply relentless pressure backed by burn and removal."),
                Archetype("Graveyard Recursion", listOf(Color.BLACK, Color.GREEN),
                    "Fill your graveyard and bring key creatures back for repeated value. A grindy midrange that trades all day and rebuilds from the yard with desert and recursion payoffs."),
                Archetype("Mounts / Ferocious", listOf(Color.RED, Color.GREEN),
                    "Ramp into big creatures with power 4 or greater, saddle your Mounts, and crash in. A ferocious aggro-midrange that overpowers opponents through raw size.",
                    creatureTypes = listOf("Mount")),
                Archetype("Mounts", listOf(Color.GREEN, Color.WHITE),
                    "Saddle your Mounts to unlock powerful attack triggers, backed by removal like Throw from the Saddle. The format's premier midrange deck, going wide and tall at once.",
                    creatureTypes = listOf("Mount")),
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
