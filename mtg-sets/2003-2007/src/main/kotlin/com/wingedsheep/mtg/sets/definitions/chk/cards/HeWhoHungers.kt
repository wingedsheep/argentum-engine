package com.wingedsheep.mtg.sets.definitions.chk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Targets
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.dsl.soulshift
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MoveType

/**
 * He Who Hungers
 * {4}{B}
 * Legendary Creature — Spirit
 * 3/2
 * Flying
 * {1}, Sacrifice a Spirit: Target opponent reveals their hand. You choose a card from it. That
 * player discards that card. Activate only as a sorcery.
 * Soulshift 4
 *
 * The reveal-and-choose discard is the same gather → choose → discard pipeline as Nightmare Void,
 * with the activating player as the chooser. It may sacrifice itself (it is a Spirit), which also
 * fires its own soulshift.
 */
val HeWhoHungers = card("He Who Hungers") {
    manaCost = "{4}{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Spirit"
    power = 3
    toughness = 2
    oracleText = "Flying\n" +
        "{1}, Sacrifice a Spirit: Target opponent reveals their hand. You choose a card from it. " +
        "That player discards that card. Activate only as a sorcery.\n" +
        "Soulshift 4 (When this creature dies, you may return target Spirit card with mana value 4 or less " +
        "from your graveyard to your hand.)"

    keywords(Keyword.FLYING)

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}"),
            Costs.Sacrifice(GameObjectFilter.Permanent.withSubtype(Subtype.SPIRIT))
        )
        val opponent = target(Targets.Opponent)
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, opponent.asPlayer), revealed = true)
            val discarded = chooseExactly(1, hand, prompt = "Choose a card for that player to discard")
            move(discarded, CardDestination.ToZone(Zone.GRAVEYARD, opponent.asPlayer),
                moveType = MoveType.Discard)
        }
        timing = TimingRule.SorcerySpeed
    }

    soulshift(4)

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "114"
        artist = "Kev Walker"
        imageUri = "https://cards.scryfall.io/normal/front/1/c/1ccf0f8d-f988-45ec-b75b-d97206326cfb.jpg?1783944315"
    }
}
