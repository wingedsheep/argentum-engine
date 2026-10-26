package com.wingedsheep.mtg.sets.definitions.khm.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.effects.MayPlayExpiry
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Jorn, God of Winter // Kaldring, the Rimestaff — Kaldheim #179 (canonical printing)
 * Legendary Snow Creature — God 3/3 // Legendary Snow Artifact · Modal double-faced card
 *
 * Back: the ability grants a this-turn may-play permission over the targeted graveyard card where it
 * lies, with the grant's `entersTapped` rider — a land played or a permanent spell cast through it
 * enters tapped. Normal timing and land-play limits apply.
 */
private val JornGodOfWinterFront = card("Jorn, God of Winter") {
    manaCost = "{2}{G}"
    colorIdentity = "BGU"
    typeLine = "Legendary Snow Creature — God"
    power = 3
    toughness = 3
    oracleText = "Whenever Jorn attacks, untap each snow permanent you control."

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.ForEachInGroup(
            GroupFilter(GameObjectFilter.Permanent.snow().youControl()),
            Effects.Untap(EffectTarget.IterationEntity),
        )
        description = "Whenever Jorn attacks, untap each snow permanent you control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Magali Villeneuve"
        flavorText = "\"Wherever snow falls, I am home.\""
        imageUri = "https://cards.scryfall.io/normal/front/c/6/c697548f-925b-405e-970a-4e78067d5c8e.jpg?1783928216"
        ruling("2021-02-05", "Jorn's triggered ability will untap Jorn itself.")
    }
}

private val KaldringTheRimestaffBack = card("Kaldring, the Rimestaff") {
    manaCost = "{1}{U}{B}"
    colorIdentity = "BGU"
    typeLine = "Legendary Snow Artifact"
    oracleText = "{T}: You may play target snow permanent card from your graveyard this turn. If you do, it enters tapped."

    activatedAbility {
        cost = Costs.Tap
        target(TargetFilter(GameObjectFilter.Permanent.snow().ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Pipeline {
            val chosen = gather(CardSource.ChosenTargets)
            run(Effects.GrantMayPlayFromExile(
                from = chosen,
                expiry = MayPlayExpiry.EndOfTurn,
                entersTapped = GameObjectFilter.Any,
            ))
        }
        description = "You may play target snow permanent card from your graveyard this turn. If you do, it enters tapped."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "179"
        artist = "Magali Villeneuve"
        flavorText = "As the sagas tell it, Jorn collects a sliver of ice from every realm he visits."
        imageUri = "https://cards.scryfall.io/normal/back/c/6/c697548f-925b-405e-970a-4e78067d5c8e.jpg?1783928216"
        ruling("2021-02-05", "A snow permanent card is any artifact, creature, enchantment, land, or planeswalker card with the supertype snow.")
        ruling("2021-02-05", "You don't play the card as Kaldring's activated ability is resolving; rather, that ability gives you permission to play that card that turn. You must follow the normal timing permissions and restrictions for the card when you play it. To play a land this way, you must have a land play available. Notably, if you activate Kaldring's ability during an opponent's turn, you won't be able to cast a spell that way unless it has flash and you won't be able to play a land that way at all.")
        ruling("2021-02-05", "You'll still pay all costs to cast a spell this way, including any additional costs. You may also pay alternative costs if any are available.")
        ruling("2021-02-05", "Even though you may play the card, it isn't in your hand, so you can't, for example, discard it or foretell it.")
        ruling("2021-02-05", "If you don't end up playing the card, it remains in your graveyard. You can't play it on future turns unless you target it again with Kaldring's ability (or something else lets you play it).")
    }
}

val JornGodOfWinter: CardDefinition = CardDefinition.modalDoubleFacedPermanent(
    frontFace = JornGodOfWinterFront,
    backFace = KaldringTheRimestaffBack,
)
