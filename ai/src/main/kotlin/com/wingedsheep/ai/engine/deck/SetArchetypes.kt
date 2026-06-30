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
 *
 * Trimmed to each set's defining archetypes; kept in lockstep with the web client's
 * `SetSynergiesOverlay.tsx` (same archetype names, colors, and descriptions).
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
                Archetype("Goblins", listOf(Color.BLACK, Color.RED),
                    "Red aggression paired with black removal. Goblin tribal synergies create explosive starts backed by efficient creature destruction.",
                    creatureTypes = listOf("Goblin")),
                Archetype("Clerics", listOf(Color.WHITE, Color.BLACK),
                    "White Clerics defend and heal while black Clerics drain life. The tribe rewards a slow, grinding game plan built on incremental advantages.",
                    creatureTypes = listOf("Cleric")),
                Archetype("Beasts", listOf(Color.RED, Color.GREEN),
                    "Green's large beasts combined with red removal create a midrange deck that wins through creature quality and raw combat dominance.",
                    creatureTypes = listOf("Beast")),
                Archetype("Elves", listOf(Color.GREEN),
                    "Elves generate mana, gain life, and pump each other. The more Elves you draft, the stronger every individual Elf becomes.",
                    creatureTypes = listOf("Elf")),
                Archetype("Soldiers / Flyers", listOf(Color.WHITE, Color.BLUE),
                    "Cheap evasive flyers and Soldier tribal put opponents on a fast clock. A tempo-aggro deck that races on the ground and in the air.",
                    creatureTypes = listOf("Soldier", "Bird")),
                Archetype("Morph", listOf(Color.BLUE, Color.GREEN),
                    "Face-down creatures conceal deadly surprises. Keep your opponent guessing while you set up devastating flip triggers and tempo plays."),
            )
        ),
        "LGN" to SetSynergies(
            setCode = "LGN",
            setName = "Legions",
            archetypes = listOf(
                Archetype("Slivers", listOf(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN),
                    "Every Sliver shares its abilities with all others. A risky but powerful tribal strategy — if the pieces come together, the hive is unstoppable.",
                    creatureTypes = listOf("Sliver")),
                Archetype("Goblins", listOf(Color.BLACK, Color.RED),
                    "Morph Goblins provide rare removal in a removal-starved set. Amplify lords reward drafting Goblins aggressively for explosive starts.",
                    creatureTypes = listOf("Goblin")),
                Archetype("Soldiers / Flyers", listOf(Color.WHITE, Color.BLUE),
                    "Soldiers and Birds combine for a tempo-evasion strategy. Fly over stalled boards while tribal lords pump your ground forces.",
                    creatureTypes = listOf("Soldier", "Bird")),
                Archetype("Clerics", listOf(Color.WHITE, Color.BLACK),
                    "The cleric synergy engine continues. In an all-creature set with no removal spells, the lifegain/drain plan is even more dominant.",
                    creatureTypes = listOf("Cleric")),
                Archetype("Elves", listOf(Color.GREEN, Color.WHITE),
                    "Elf lords that tap to pump make every Elf a threat. Amplify and provoke create an engine of growth that overwhelms with sheer numbers.",
                    creatureTypes = listOf("Elf")),
            )
        ),
        "SCG" to SetSynergies(
            setCode = "SCG",
            setName = "Scourge",
            archetypes = listOf(
                Archetype("Dragons", listOf(Color.RED),
                    "Dedicated Dragon tribal support rewards these devastating flying threats. High mana costs are a feature, not a bug.",
                    creatureTypes = listOf("Dragon")),
                Archetype("Goblins", listOf(Color.BLACK, Color.RED),
                    "Black removal plus Goblin tribal synergies. Storm spells can steal games out of nowhere.",
                    creatureTypes = listOf("Goblin")),
                Archetype("Wizards", listOf(Color.BLUE, Color.RED),
                    "Draw cards proportional to your highest mana cost for massive card advantage. Wizard synergies from the block still anchor the deck.",
                    creatureTypes = listOf("Wizard")),
                Archetype("Landcycling", listOf(Color.WHITE, Color.GREEN),
                    "Landcycling creatures fix your mana early and provide large bodies late. Smooths draws across multiple colors."),
                Archetype("Ramp", listOf(Color.BLUE, Color.GREEN),
                    "Ramp into large green creatures, then leverage their high mana cost for powerful draw spells. Bury opponents in card advantage."),
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
                Archetype("Saproling Sacrifice", listOf(Color.BLACK, Color.GREEN),
                    "Generate Saprolings and sacrifice them for value. Thallids and fungus creatures create a self-sustaining engine of tokens and drain effects."),
                Archetype("Wizard Spells", listOf(Color.BLUE, Color.RED),
                    "Wizards grow stronger with every instant and sorcery you cast. Build a critical mass of spells and Wizards for explosive prowess-style turns.",
                    creatureTypes = listOf("Wizard")),
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
                Archetype("Lander Ramp", listOf(Color.BLUE, Color.GREEN),
                    "Crack Lander tokens for mana and fixing, ramping into the format's biggest threats. The premier two-for-one ramp deck that out-resources the table and splashes bombs."),
                Archetype("Void", listOf(Color.BLACK, Color.RED),
                    "Sacrifice your own creatures to switch on void payoffs, turning every permanent that dies into value. A grindy sacrifice-aggro deck that controls the board while bleeding the opponent."),
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
                Archetype("Crime", listOf(Color.BLUE, Color.BLACK),
                    "Commit crimes — target opponents and their stuff — to snowball value and disruption. A tempo-control deck that drains the opponent while answering their threats."),
                Archetype("Ramp Plot", listOf(Color.BLUE, Color.GREEN),
                    "Ramp and discount plot cards to set up powerful future turns, then land the format's biggest threats ahead of schedule. A controlling midrange that out-values the table."),
                Archetype("Mercenaries", listOf(Color.RED, Color.WHITE),
                    "Go wide with Mercenary tokens and aggressive bodies, then convert the swarm to damage with go-wide payoffs and mass pump. A fast, token-fueled aggro deck.",
                    creatureTypes = listOf("Mercenary")),
                Archetype("Outlaws", listOf(Color.BLACK, Color.RED),
                    "The dedicated outlaw deck — Assassins, Mercenaries, Pirates, Rogues, and Warlocks trigger aggressive payoffs. Apply relentless pressure backed by burn and removal."),
                Archetype("Mounts", listOf(Color.GREEN, Color.WHITE),
                    "Saddle your Mounts to unlock powerful attack triggers, backed by removal like Throw from the Saddle. The format's premier midrange deck, going wide and tall at once.",
                    creatureTypes = listOf("Mount")),
            )
        ),
        "DSK" to SetSynergies(
            setCode = "DSK",
            setName = "Duskmourn: House of Horror",
            archetypes = listOf(
                Archetype("Eerie Enchantments", listOf(Color.WHITE, Color.BLUE),
                    "Trigger Eerie by playing enchantments and unlocking Rooms. A tempo-value deck that builds an enchantment engine, churning out Glimmer tokens and incremental advantage while flyers close in the air."),
                Archetype("Survival", listOf(Color.GREEN, Color.WHITE),
                    "Keep creatures untapped through your second main phase to switch on Survival payoffs. A go-tall midrange that piles on +1/+1 counters and rewards a sturdy, defensive board."),
                Archetype("Manifest Dread", listOf(Color.BLUE, Color.GREEN),
                    "Manifest dread to flood the board with face-down 2/2s while stocking your graveyard. A value-midrange that converts card selection into board presence and creatures-entering payoffs."),
                Archetype("Delirium", listOf(Color.BLACK, Color.GREEN),
                    "Fill your graveyard with four or more card types to unlock Delirium bonuses. A grindy graveyard-value deck that mills, sacrifices, and recurs threats to out-attrition the table."),
                Archetype("Sacrifice Aggro", listOf(Color.BLACK, Color.RED),
                    "Throw expendable creatures and tokens at sacrifice outlets for value and reach. An aggressive deck that drains the opponent and turns dying bodies into damage."),
                Archetype("Impending", listOf(Color.RED, Color.GREEN),
                    "Deploy big threats early with Impending time counters, then ramp into the format's largest creatures. A midrange-ramp deck that overpowers opponents once its haymakers come online."),
            )
        ),
        "STX" to SetSynergies(
            setCode = "STX",
            setName = "Strixhaven: School of Mages",
            archetypes = listOf(
                Archetype("Silverquill", listOf(Color.WHITE, Color.BLACK),
                    "Build a wide board of Inkling tokens, then pile on +1/+1 counters and push through with evasion and lifegain. An aggressive counters-and-tokens deck that closes the game fast."),
                Archetype("Prismari", listOf(Color.BLUE, Color.RED),
                    "Cast a high volume of instants and sorceries to trigger magecraft, churning out Treasure and big Elemental payoffs. A spell-slinging tempo deck that snowballs with every cast."),
                Archetype("Witherbloom", listOf(Color.BLACK, Color.GREEN),
                    "Drain the opponent with Pests and lifeloss payoffs, sacrificing tokens for value while recurring threats from the graveyard. A grindy attrition deck that bleeds the table dry."),
                Archetype("Lorehold", listOf(Color.RED, Color.WHITE),
                    "Recur instants and sorceries from your graveyard and reward casting historic spells, turning Spirit tokens and value loops into relentless pressure. A spells-matter midrange deck."),
                Archetype("Quandrix", listOf(Color.GREEN, Color.BLUE),
                    "Ramp into extra mana and pile +1/+1 counters onto Fractal tokens, scaling your board with the size of your mana. A go-big ramp-and-counters deck that overwhelms in the late game."),
            )
        ),
        "TMT" to SetSynergies(
            setCode = "TMT",
            setName = "Teenage Mutant Ninja Turtles",
            archetypes = listOf(
                Archetype("Blink Tempo", listOf(Color.WHITE, Color.BLUE),
                    "Bounce and blink your artifacts and creatures to re-trigger their enter-the-battlefield abilities, grinding out value while evasive Turtles and Robots seize the tempo. A patient value-control deck."),
                Archetype("Mill Control", listOf(Color.BLUE, Color.BLACK),
                    "Mill your opponents and steal their best cards, leaning on removal and evasion to control the game. A grindy control deck that wins by turning the opponent's own library against them."),
                Archetype("Graveyard Value", listOf(Color.BLACK, Color.GREEN),
                    "Fill your graveyard, then reanimate fallen creatures and pile on +1/+1 counters. A grindy recursion deck that out-attritions the table and simply refuses to stay dead."),
                Archetype("Mutant Beatdown", listOf(Color.RED, Color.GREEN),
                    "Deploy hasty, trampling threats and flood the board with 2/2 Mutant tokens, then swing with overwhelming force. A fast midrange-aggro deck that wins through raw board presence."),
                Archetype("Alliance Aggro", listOf(Color.RED, Color.WHITE),
                    "Go wide so every creature that enters triggers Alliance, then take extra combat phases to swing again and again. An aggressive go-wide deck built on relentless attacks."),
                Archetype("Mutant Ramp", listOf(Color.GREEN, Color.BLUE),
                    "Ramp and dig to cast Mutant, Ninja, and Turtle creatures from the top of your library, each entering with a bonus +1/+1 counter. A creature-type value deck that snowballs card advantage."),
            )
        ),
        "INV" to SetSynergies(
            setCode = "INV",
            setName = "Invasion",
            archetypes = listOf(
                Archetype("Bird Tempo", listOf(Color.WHITE, Color.BLUE),
                    "Take to the skies with evasive fliers and kicker-pumped Birds, riding tempo to victory. A flier-centric deck where kicker turns your lord into a finisher."),
                Archetype("Tempo Control", listOf(Color.BLUE, Color.BLACK),
                    "Counter spells and bounce threats while bleeding the opponent with discard. A tempo-control deck that answers everything and grinds out the long game."),
                Archetype("Sacrifice Discard", listOf(Color.BLACK, Color.RED),
                    "Strip the opponent's hand with discard and evasive damage, then turn dying creatures into reanimation value. An aggressive disruption deck that attacks resources and life total at once."),
                Archetype("Kicker Aggro", listOf(Color.RED, Color.GREEN),
                    "Give your creatures haste and ramp into kicker payoffs, swinging the moment threats hit the board. A fast aggro-ramp deck powered by Kavu and big kicked spells."),
                Archetype("Aura Legends", listOf(Color.GREEN, Color.WHITE),
                    "Suit up a creature with powerful Auras like Armadillo Cloak and tutor up your best legends. A midrange deck that goes tall and grinds out incremental value."),
                Archetype("Domain", listOf(Color.WHITE, Color.BLUE, Color.BLACK, Color.RED, Color.GREEN),
                    "Splash every color to switch on domain and unleash the Coalition Dragons, the format's premier finishers. A greedy five-color goodstuff deck built on mana fixing."),
            )
        ),
        "LTR" to SetSynergies(
            setCode = "LTR",
            setName = "The Lord of the Rings: Tales of Middle-earth",
            archetypes = listOf(
                Archetype("Spirits", listOf(Color.WHITE, Color.BLACK),
                    "Phase your Spirits out of removal and back in to spawn flying tokens. A resilient tempo deck that protects its threats and wins through the air."),
                Archetype("Spellslinger", listOf(Color.BLUE, Color.RED),
                    "Cast a high volume of instants and sorceries to trigger modal payoffs and recur your spells. A spell-matters tempo-control deck that out-values the table."),
                Archetype("Orc Army", listOf(Color.BLACK, Color.RED),
                    "Amass Orc Army and pump your token horde, where every +1/+1 counter lands amplified. A go-wide aggro deck backed by menace and removal that swarms the board."),
                Archetype("Food Sacrifice", listOf(Color.BLACK, Color.GREEN),
                    "Turn dying creatures and Spider combat into Food, then sacrifice it for value and reach. A grindy graveyard-and-Food midrange deck that out-attritions the opponent."),
                Archetype("Rohirrim Aggro", listOf(Color.RED, Color.WHITE),
                    "Flood the board with aggressive Humans and grant double strike on entry, then swing for the win. A fast go-wide combat deck built on Rohirrim synergies."),
                Archetype("Ring-bearers", listOf(Color.GREEN, Color.WHITE),
                    "Lean on legendary creatures and \"the Ring tempts you\" triggers, growing an evasive Ring-bearer into a game-ender. A midrange deck built on legends and steady Ring value."),
            )
        ),
        "SOS" to SetSynergies(
            setCode = "SOS",
            setName = "Secrets of Strixhaven",
            archetypes = listOf(
                Archetype("Silverquill", listOf(Color.WHITE, Color.BLACK),
                    "Sling instants and sorceries with casualty, sacrificing a creature to copy each spell while evasive fliers carry the game. A spell-and-sacrifice tempo deck that doubles up on its best cards."),
                Archetype("Prismari", listOf(Color.BLUE, Color.RED),
                    "Chain instants and sorceries to fuel storm, copying each spell for every one cast before it that turn. A spell-velocity tempo deck that snowballs into an explosive turn."),
                Archetype("Witherbloom", listOf(Color.BLACK, Color.GREEN),
                    "Flood the board with creatures to power affinity, casting your spells at a discount while lifegain and death payoffs grind the opponent down. A creatures-and-spells midrange deck."),
                Archetype("Lorehold", listOf(Color.RED, Color.WHITE),
                    "Give your instants and sorceries miracle and reward drawing them off the top, looting to dig for the perfect topdeck. A draw-matters tempo deck that casts spells at a steep discount."),
                Archetype("Quandrix", listOf(Color.GREEN, Color.BLUE),
                    "Ramp into expensive instants and sorceries that cascade for free value, snowballing card advantage with Increment counters. A go-big spells-matter ramp deck."),
            )
        ),
        "ECL" to SetSynergies(
            setCode = "ECL",
            setName = "Lorwyn Eclipsed",
            archetypes = listOf(
                Archetype("Kithkin", listOf(Color.GREEN, Color.WHITE),
                    "Flood the board with 1/1 Kithkin tokens, then use Convoke to cast expensive spells for free. Mass pump effects turn the token horde lethal.",
                    creatureTypes = listOf("Kithkin")),
                Archetype("Merfolk", listOf(Color.WHITE, Color.BLUE),
                    "Merfolk untap each other after combat and chain together card draw. Control the pace of the game with bounce and tempo plays while building an unstoppable Merrow tide.",
                    creatureTypes = listOf("Merfolk")),
                Archetype("Faeries", listOf(Color.BLUE, Color.BLACK),
                    "Operate entirely at flash speed — drop Faeries at end of turn, steal blockers, and lock out opponents with aerial superiority. A control deck that wins with evasion.",
                    creatureTypes = listOf("Faerie")),
                Archetype("Boggarts", listOf(Color.BLACK, Color.RED),
                    "Relentless Boggart aggro backed by Wither. Your attackers deal damage as -1/-1 counters, permanently shrinking blockers. Blight provides flexible sacrifice payoffs.",
                    creatureTypes = listOf("Goblin")),
                Archetype("Elementals", listOf(Color.BLUE, Color.RED),
                    "Flamekin Elementals reward casting instants and sorceries. Evoke provides flexible tempo plays — pay the cheap cost for a burst of value, or go full price for a permanent threat.",
                    creatureTypes = listOf("Elemental")),
                Archetype("Changelings", listOf(Color.BLUE, Color.GREEN),
                    "Changelings count as every creature type simultaneously, triggering Kithkin, Merfolk, Elf, and Faerie payoffs all at once. A tribal swiss-army-knife deck with enormous flexibility.",
                    creatureTypes = listOf("Shapeshifter")),
            )
        ),
        "FIN" to SetSynergies(
            setCode = "FIN",
            setName = "Final Fantasy",
            archetypes = listOf(
                Archetype("Artifacts", listOf(Color.WHITE, Color.BLUE),
                    "Flood the board with artifacts — Equipment, Treasures, and artifact creatures — then cash in go-wide payoffs while card filtering keeps the gas flowing. A tempo-value deck that snowballs as its trinkets pile up."),
                Archetype("Sacrifice", listOf(Color.WHITE, Color.BLACK),
                    "Feed a steady stream of creatures and tokens to sacrifice outlets, wringing two-for-one value and drain out of everything that dies. A grindy attrition deck built on the set's villains."),
                Archetype("Equipment Aggro", listOf(Color.WHITE, Color.RED),
                    "Suit up aggressive creatures with Equipment — much of it attached to a body already — and push damage through with combat keywords like double strike. A fast go-tall aggro deck that's hard to block profitably."),
                Archetype("Go-Wide Tokens", listOf(Color.WHITE, Color.GREEN),
                    "Summons and lower-rarity Eikons flood the board with tokens, then mass pump and anthem effects close the game before attrition sets in. A go-wide aggro deck that ends games early."),
                Archetype("Control", listOf(Color.BLUE, Color.BLACK),
                    "Prolong the game with removal and counters, recurring your spells with flashback before taking over with powerful late-game bombs. A grindy control deck that out-values the table."),
                Archetype("Big Spells", listOf(Color.BLUE, Color.RED),
                    "Lean on expensive instants and sorceries, where spending four or more mana on a noncreature spell unlocks extra value. A spell-velocity tempo deck that rewards going big."),
                Archetype("Towns Ramp", listOf(Color.BLUE, Color.GREEN),
                    "Play Towns and other nonbasic lands for repeatable value, ramping into the format's largest threats. A two-for-one ramp deck that out-resources opponents in the late game."),
                Archetype("Black Mages", listOf(Color.BLACK, Color.RED),
                    "A cheap removal package spits out Wizard tokens that add reach and combat math, recreating the Black Mage dream of slinging elemental damage. An aggressive tempo deck backed by relentless burn.",
                    creatureTypes = listOf("Wizard")),
                Archetype("Graveyard", listOf(Color.BLACK, Color.GREEN),
                    "Fill your graveyard, then grind the game out with recursion and graveyard payoffs that flip into game-ending threats. A patient value-midrange that simply refuses to stay dead."),
                Archetype("Landfall", listOf(Color.RED, Color.GREEN),
                    "Trigger landfall every time a land enters, turning even late-game land draws into damage with angry Chocobos and big beaters. A ramp-and-aggro deck that overwhelms with raw board presence."),
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
