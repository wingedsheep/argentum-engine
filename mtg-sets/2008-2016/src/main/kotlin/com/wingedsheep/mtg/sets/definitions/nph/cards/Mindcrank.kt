package com.wingedsheep.mtg.sets.definitions.nph.cards

import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Mindcrank — New Phyrexia #144
 * {2} · Artifact · Uncommon
 *
 * Whenever an opponent loses life, that player mills that many cards. (Damage causes loss of life.)
 *
 * `Triggers.anOpponent.losesLife()` fires once per life-loss event (damage and paid life included);
 * the amount rides on the event via `DynamicAmounts.triggerLifeLost()`, and
 * [Player.TriggeringPlayer] sends the mill at the opponent who lost the life.
 */
val Mindcrank = card("Mindcrank") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "Whenever an opponent loses life, that player mills that many cards. " +
        "(Damage causes loss of life.)"

    triggeredAbility {
        trigger = Triggers.anOpponent.losesLife()
        effect = Patterns.Library.mill(
            count = DynamicAmounts.triggerLifeLost(),
            target = EffectTarget.PlayerRef(Player.TriggeringPlayer)
        )
        description = "Whenever an opponent loses life, that player mills that many cards."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "144"
        artist = "Chris Rahn"
        flavorText = "\"Let go of your memories and be reborn.\"\n—Argent Etchings, plate 106, passage 27"
        imageUri = "https://cards.scryfall.io/normal/front/d/1/d13a5ae0-d76a-4430-98c1-47a19e615e2c.jpg?1783941294"
        ruling(
            "2017-11-17",
            "Damage dealt to an opponent usually causes that opponent to lose life. An opponent paying life also causes loss of life."
        )
    }
}
