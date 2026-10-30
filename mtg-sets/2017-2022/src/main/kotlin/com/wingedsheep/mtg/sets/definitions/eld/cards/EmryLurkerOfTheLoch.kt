package com.wingedsheep.mtg.sets.definitions.eld.cards

import com.wingedsheep.sdk.core.CardType
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Emry, Lurker of the Loch — Throne of Eldraine #43
 * {2}{U} · Legendary Creature — Merfolk Wizard 1/2
 *
 * Affinity for artifacts
 * When Emry enters, mill four cards.
 * {T}: Choose target artifact card in your graveyard. You may cast that card this turn.
 *
 * The cast permission is Heiko Yamazaki's: a this-turn may-play grant over the targeted graveyard
 * card where it lies, cast-only (an artifact land can't be played this way). Normal costs and
 * timing still apply.
 */
val EmryLurkerOfTheLoch = card("Emry, Lurker of the Loch") {
    manaCost = "{2}{U}"
    colorIdentity = "U"
    typeLine = "Legendary Creature — Merfolk Wizard"
    power = 1
    toughness = 2
    oracleText = "Affinity for artifacts (This spell costs {1} less to cast for each artifact you control.)\n" +
        "When Emry enters, mill four cards.\n" +
        "{T}: Choose target artifact card in your graveyard. You may cast that card this turn. " +
        "(You still pay its costs. Timing rules still apply.)"

    keywordAbility(KeywordAbility.Affinity(CardType.ARTIFACT))

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Patterns.Library.mill(4)
        description = "When Emry enters, mill four cards."
    }

    activatedAbility {
        cost = Costs.Tap
        target(TargetFilter(GameObjectFilter.Artifact.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(
                from = chosen,
                expiry = MayPlayExpiry.EndOfTurn,
                nonLandOnly = true,
            ))
        }
        description = "Choose target artifact card in your graveyard. You may cast that card this turn."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "43"
        artist = "Livia Prima"
        imageUri = "https://cards.scryfall.io/normal/front/b/f/bf4b9a8a-b42a-46fb-b0d0-9cf800f63c8a.jpg?1783932660"
        ruling("2019-10-04", "Casting the target card causes it to leave your graveyard and become a new object. You can't cast it again if it returns to your graveyard this turn.")
        ruling("2019-10-04", "If Emry's last ability targets an artifact land card, you can't play it. Effects that allow you to \"cast\" a card don't allow you to play a land card.")
        ruling("2019-10-04", "You must follow the normal timing permissions and restrictions for the target artifact card. Unless it has flash, you'll most likely only be able to cast it during your main phase while the stack is empty.")
    }
}
