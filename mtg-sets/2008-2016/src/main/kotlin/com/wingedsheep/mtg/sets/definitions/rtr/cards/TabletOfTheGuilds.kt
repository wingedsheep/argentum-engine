package com.wingedsheep.mtg.sets.definitions.rtr.cards

import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.ChoiceType
import com.wingedsheep.sdk.scripting.EntersWithChoice
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Tablet of the Guilds
 * {2}
 * Artifact
 * As this artifact enters, choose two colors.
 * Whenever you cast a spell, if it's at least one of the chosen colors, you gain 1 life for each
 * of the chosen colors it is.
 *
 * `EntersWithChoice(COLOR, colorCount = 2)` records both colors on the Tablet. The intervening "if"
 * is `sharingChosenColorWithSource()`, which with two chosen colors means "at least one of them";
 * the payoff walks the chosen colors with [Effects.ForEachChosenColor] and gains 1 for each one
 * the spell is (`withChosenColor()` reads the color being visited). The spell's colors are read
 * as the trigger resolves, wherever the card is then (it still resolves if the spell was countered);
 * if the Tablet has left, its chosen colors come from its last-known information.
 */
val TabletOfTheGuilds = card("Tablet of the Guilds") {
    manaCost = "{2}"
    typeLine = "Artifact"
    oracleText = "As this artifact enters, choose two colors.\n" +
        "Whenever you cast a spell, if it's at least one of the chosen colors, you gain 1 life for each of the chosen colors it is."

    replacementEffect(EntersWithChoice(ChoiceType.COLOR, colorCount = 2))

    triggeredAbility {
        trigger = Triggers.you.casts()
        interveningIf = Conditions.TriggeringSpellMatches(GameObjectFilter.Any.sharingChosenColorWithSource())
        effect = Effects.ForEachChosenColor(
            effect = Effects.If(
                Conditions.TriggeringSpellMatches(GameObjectFilter.Any.withChosenColor()),
                Effects.GainLife(1)
            )
        )
        description = "Whenever you cast a spell, if it's at least one of the chosen colors, you gain 1 life for each of the chosen colors it is."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "235"
        artist = "Nic Klein"
        flavorText = "It is rumored the Azorius paid an exorbitant sum to ensure its symbol would appear at the top."
        imageUri = "https://cards.scryfall.io/normal/front/6/b/6b006384-6fb6-4129-b1d2-7674d1141f8f.jpg?1783940323"
    }
}
