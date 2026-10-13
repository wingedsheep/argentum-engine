package com.wingedsheep.mtg.sets.definitions.dmu.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MayCastSelfFromZones
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Rona, Sheoldred's Faithful
 * {1}{U}{B}{B}
 * Legendary Creature — Human Wizard
 * 3/4
 * Whenever you cast an instant or sorcery spell, each opponent loses 1 life.
 * You may cast this card from your graveyard by discarding two cards in addition to paying its
 * other costs.
 */
val RonaSheoldredsFaithful = card("Rona, Sheoldred's Faithful") {
    manaCost = "{1}{U}{B}{B}"
    colorIdentity = "UB"
    typeLine = "Legendary Creature — Human Wizard"
    power = 3
    toughness = 4
    oracleText = "Whenever you cast an instant or sorcery spell, each opponent loses 1 life.\n" +
        "You may cast this card from your graveyard by discarding two cards in addition to paying its other costs."

    triggeredAbility {
        trigger = Triggers.you.casts(GameObjectFilter.InstantOrSorcery)
        effect = Effects.LoseLife(1, EffectTarget.PlayerRef(Player.EachOpponent))
    }

    // Self-referential cast-from-graveyard permission with a bundled additional cost; normal
    // (sorcery-speed) creature timing and the {1}{U}{B}{B} mana cost still apply.
    staticAbility {
        ability = MayCastSelfFromZones(
            zones = listOf(Zone.GRAVEYARD),
            additionalCost = Costs.additional.DiscardCards(2)
        )
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "216"
        artist = "Ryan Alexander Lee"
        flavorText = "\"Consider yourself lucky. Not everyone is granted the gift of *compleation*.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/c/cc8331ab-adc9-45c6-9728-66afc72d901c.jpg?1783921276"
        ruling("2022-09-09", "The casting permission granted by Rona's last ability doesn't change when you may cast it from your graveyard.")
        ruling("2022-09-09", "If Rona is countered or dies after being cast from your graveyard, it returns to the graveyard. It may be cast this way again later.")
    }
}
