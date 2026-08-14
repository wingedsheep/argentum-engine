package com.wingedsheep.mtg.sets.definitions.msh.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ModalEffect
import com.wingedsheep.sdk.scripting.effects.Mode
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter

/**
 * Black Panther, Vanguard — Marvel Super Heroes #207
 * {2}{G}{W} · Legendary Creature — Human Warrior Hero · 4/4
 *
 * Whenever another nontoken Hero you control enters, choose one —
 * • Create a 1/1 white Soldier creature token.
 * • Creatures you control get +1/+1 until end of turn.
 *
 * Modeling notes:
 *  - "Another nontoken Hero you control" is an OTHER-bound [Triggers.entersBattlefield] over
 *    `Creature.withSubtype(HERO).youControl().nontoken()`. The OTHER binding supplies the
 *    "another", so Black Panther entering never triggers himself; the `nontoken()` clause means
 *    the Soldier / Hero tokens this deck makes don't feed him. Note the printed filter is a
 *    *creature* Hero — every Hero in the set is a creature, and reading it as one keeps the
 *    trigger off noncreature permanents that pick up the subtype.
 *  - The mode choice is made as the ability goes on the stack (CR 603.3c), which is why this is
 *    a [ModalEffect] with `chooseCount = 1` rather than a resolution-time branch. Neither mode
 *    targets, so both are [Mode.noTarget].
 *  - Mode 2 is [Patterns.Group.modifyStatsForAll] over creatures you control — the affected set
 *    is locked in when the ability resolves, so creatures that arrive later in the turn don't
 *    get the bonus.
 */
val BlackPantherVanguard = card("Black Panther, Vanguard") {
    manaCost = "{2}{G}{W}"
    colorIdentity = "WG"
    typeLine = "Legendary Creature — Human Warrior Hero"
    power = 4
    toughness = 4
    oracleText = "Whenever another nontoken Hero you control enters, choose one —\n" +
        "• Create a 1/1 white Soldier creature token.\n" +
        "• Creatures you control get +1/+1 until end of turn."

    triggeredAbility {
        trigger = Triggers.entersBattlefield(
            filter = GameObjectFilter.Creature.withSubtype(Subtype.HERO).youControl().nontoken(),
            binding = TriggerBinding.OTHER,
        )
        effect = ModalEffect(
            modes = listOf(
                Mode.noTarget(
                    Effects.CreateToken(
                        power = 1,
                        toughness = 1,
                        colors = setOf(Color.WHITE),
                        creatureTypes = setOf("Soldier"),
                        imageUri = "https://cards.scryfall.io/normal/front/e/c/ecd686bf-d14b-491c-b0c5-88fc8f0472f9.jpg?1783902804",
                    ),
                    "Create a 1/1 white Soldier creature token.",
                ),
                Mode.noTarget(
                    Patterns.Group.modifyStatsForAll(1, 1, GroupFilter.AllCreaturesYouControl),
                    "Creatures you control get +1/+1 until end of turn.",
                ),
            ),
            chooseCount = 1,
        )
        description = "Whenever another nontoken Hero you control enters, choose one — " +
            "• Create a 1/1 white Soldier creature token. " +
            "• Creatures you control get +1/+1 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "207"
        artist = "Aniekan Udofia"
        flavorText = "\"Wakanda forever!\""
        imageUri = "https://cards.scryfall.io/normal/front/9/e/9e308f77-206b-4c9d-bfba-f4c476ea574a.jpg?1783902905"
    }
}
