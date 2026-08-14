package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.MustAttack
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/** Town Gossipmonger // Incited Rabble (Shadows over Innistrad). */
private val TownGossipmongerFront = card("Town Gossipmonger") {
    manaCost = "{W}"
    colorIdentity = "WR"
    typeLine = "Creature — Human"
    oracleText = "{T}, Tap an untapped creature you control: Transform this creature."
    power = 1
    toughness = 1

    activatedAbility {
        cost = Costs.Composite(
            Costs.Tap,
            Costs.TapAnotherPermanent(GameObjectFilter.Creature)
        )
        effect = TransformEffect(EffectTarget.Self)
        description = "Transform this creature."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "John Stanko"
        flavorText = "\"You'll never believe what I heard.\""
        imageUri = "https://cards.scryfall.io/normal/front/d/2/d238d324-8c88-43ca-b203-133830d29447.jpg?1783937812"
        ruling("2025-01-24", "You can tap any untapped creature you control, including one you haven't controlled continuously since the beginning of your most recent turn, to pay the cost of Town Gossipmonger's activated ability. You must have controlled Town Gossipmonger continuously since the beginning of your most recent turn, however.")
        ruling("2025-01-24", "Incited Rabble's controller still chooses which player, planeswalker, or battle it attacks.")
        ruling("2025-01-24", "If, during its controller's declare attackers step, Incited Rabble is tapped or is affected by a spell or ability that says it can't attack, then it doesn't attack. If there's a cost associated with having Incited Rabble attack, its controller isn't forced to pay that cost, so it doesn't have to attack in that case either. Note that transforming Town Gossipmonger won't untap it.")
    }
}

private val IncitedRabble = card("Incited Rabble") {
    manaCost = ""
    colorIdentity = "WR"
    colorIndicator = "R"
    typeLine = "Creature — Human"
    oracleText = "This creature attacks each combat if able.\n{2}: This creature gets +1/+0 until end of turn."
    power = 2
    toughness = 3

    staticAbility {
        ability = MustAttack()
    }

    activatedAbility {
        cost = Costs.Mana("{2}")
        effect = Effects.ModifyStats(1, 0, EffectTarget.Self)
        description = "This creature gets +1/+0 until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "46"
        artist = "John Stanko"
        flavorText = "Rumors are sparks: gather enough of them and something's going to catch fire."
        imageUri = "https://cards.scryfall.io/normal/back/d/2/d238d324-8c88-43ca-b203-133830d29447.jpg?1783937812"
    }
}

val TownGossipmonger: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = TownGossipmongerFront,
    backFace = IncitedRabble,
)
