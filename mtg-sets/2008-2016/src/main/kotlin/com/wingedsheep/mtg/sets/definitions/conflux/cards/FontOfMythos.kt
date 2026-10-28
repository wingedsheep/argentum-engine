package com.wingedsheep.mtg.sets.definitions.conflux.cards

import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Font of Mythos
 * {4}
 * Artifact
 * At the beginning of each player's draw step, that player draws two additional cards.
 *
 * The Rites of Flourishing shape: a trigger on every player's draw step that draws for the
 * player whose draw step it is, resolving after the turn's normal draw.
 */
val FontOfMythos = card("Font of Mythos") {
    manaCost = "{4}"
    colorIdentity = ""
    typeLine = "Artifact"
    oracleText = "At the beginning of each player's draw step, that player draws two additional cards."

    triggeredAbility {
        trigger = Triggers.anyPlayer.beginningOf(Step.DRAW)
        effect = Effects.DrawCards(2, EffectTarget.PlayerRef(Player.TriggeringPlayer))
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "136"
        artist = "Dave Allsop"
        flavorText = "Those who drink from Malfegor's cup are tainted with hunger and stained with lies."
        imageUri = "https://cards.scryfall.io/normal/front/c/1/c198caf8-27ab-4300-841b-507e1b0ce9b3.jpg?1783942462"
        ruling("2013-04-15", "The triggered ability is put onto the stack after you have already drawn your card for the turn.")
    }
}
