package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Flitting Guerrilla
 * {2}{B}
 * Creature — Faerie Rogue
 * 2/2
 *
 * Flying
 * When this creature dies, each player mills two cards. Then you may exile this card. When you do,
 * put target creature or battle card from your graveyard on top of your library.
 *
 * The exile is the optional action of a [ReflexiveTriggerEffect] (CR 603.12), gated on the card
 * still being in the graveyard; the target is chosen when the reflexive ability goes on the stack,
 * so the exiled Guerrilla itself is never a legal target.
 */
val FlittingGuerrilla = card("Flitting Guerrilla") {
    manaCost = "{2}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Faerie Rogue"
    power = 2
    toughness = 2
    oracleText = "Flying\n" +
        "When this creature dies, each player mills two cards. Then you may exile this card. " +
        "When you do, put target creature or battle card from your graveyard on top of your library. " +
        "(To mill two cards, a player puts the top two cards of their library into their graveyard.)"

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.self.dies()
        effect = Patterns.Library.mill(2, EffectTarget.PlayerRef(Player.Each)) then
            Effects.ReflexiveTrigger(
                action = Effects.Exile(EffectTarget.Self, fromZone = Zone.GRAVEYARD),
                optional = true,
                descriptionOverride = "You may exile this card. When you do, put target creature or " +
                    "battle card from your graveyard on top of your library."
            ) {
                val card = target(
                    TargetFilter(
                        (GameObjectFilter.Creature or GameObjectFilter.Battle).ownedByYou(),
                        zone = Zone.GRAVEYARD
                    )
                )
                effect = Effects.PutOnTopOfLibrary(card)
            }
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "105"
        artist = "Francisco Miyara"
        imageUri = "https://cards.scryfall.io/normal/front/9/1/9184bc57-0a16-4abf-a13a-6a03a175c28a.jpg?1783917010"
    }
}
