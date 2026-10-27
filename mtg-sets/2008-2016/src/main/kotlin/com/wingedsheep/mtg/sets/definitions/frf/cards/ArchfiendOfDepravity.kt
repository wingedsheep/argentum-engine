package com.wingedsheep.mtg.sets.definitions.frf.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Archfiend of Depravity
 * {3}{B}{B}
 * Creature — Demon
 * 5/4
 * Flying
 * At the beginning of each opponent's end step, that player chooses up to two creatures they
 * control, then sacrifices the rest.
 *
 * The opponent whose end step it is is the trigger's `TriggeringPlayer`: their creatures are
 * gathered, *they* make the up-to-two keep choice on resolution (no targeting, per the ruling),
 * and the remainder is sacrificed — [com.wingedsheep.mtg.sets.definitions.rav.cards.RaziasPurification]'s
 * keep-and-sacrifice split with an up-to count. `alwaysPrompt` keeps the choice even when they
 * control two or fewer creatures, since sparing fewer than two is legal.
 */
val ArchfiendOfDepravity = card("Archfiend of Depravity") {
    manaCost = "{3}{B}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Demon"
    oracleText = "Flying\n" +
        "At the beginning of each opponent's end step, that player chooses up to two creatures they control, " +
        "then sacrifices the rest."
    power = 5
    toughness = 4

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.anOpponent.beginningOf(Step.END)
        effect = Effects.Pipeline {
            val creatures = gather(
                CardSource.ControlledPermanents(
                    player = Player.TriggeringPlayer,
                    filter = GameObjectFilter.Creature,
                )
            )
            val (_, sacrificed) = chooseUpToSplit(
                2,
                from = creatures,
                chooser = Chooser.TriggeringPlayer,
                prompt = "Choose up to two creatures you control to keep; the rest are sacrificed.",
                selectedLabel = "Keep",
                remainderLabel = "Sacrifice",
                useTargetingUI = true,
                alwaysPrompt = true,
            )
            sacrifice(sacrificed)
        }
        description = "At the beginning of each opponent's end step, that player chooses up to two creatures " +
            "they control, then sacrifices the rest."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "62"
        artist = "Daarken"
        flavorText = "\"Why would I kill you all? Who then would be left to worship me?\""
        imageUri = "https://cards.scryfall.io/normal/front/2/2/22e343b5-37d6-44f6-be24-66313f18c246.jpg?1783938701"

        ruling(
            "2014-11-24",
            "The opponent may choose to spare one or no creatures. If the player doesn't choose any creatures, " +
                "they will sacrifice all creatures they control."
        )
        ruling(
            "2014-11-24",
            "The opponent chooses which creatures to spare, if any, as the ability resolves. This choice doesn't " +
                "target the creatures."
        )
    }
}
