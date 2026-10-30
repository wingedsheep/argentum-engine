package com.wingedsheep.mtg.sets.definitions.thb.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Binding of the Titans
 * {1}{G}
 * Enchantment — Saga
 *
 * (As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)
 * I — Each player mills three cards.
 * II — Exile up to two target cards from graveyards. For each creature card exiled this way, you
 *      gain 1 life.
 * III — Return target creature or land card from your graveyard to your hand.
 *
 * Chapter II is Graveyard Glutton's shape: the life gain counts the creature cards in the pile that
 * actually moved ([moveTracked]), so a target that left its graveyard in response gains nothing.
 */
val TheBindingOfTheTitans = card("The Binding of the Titans") {
    manaCost = "{1}{G}"
    colorIdentity = "G"
    typeLine = "Enchantment — Saga"
    oracleText = "(As this Saga enters and after your draw step, add a lore counter. Sacrifice after III.)\n" +
        "I — Each player mills three cards.\n" +
        "II — Exile up to two target cards from graveyards. For each creature card exiled this way, " +
        "you gain 1 life.\n" +
        "III — Return target creature or land card from your graveyard to your hand."

    sagaChapter(1) {
        effect = Patterns.Library.mill(3, EffectTarget.PlayerRef(Player.Each))
    }

    sagaChapter(2) {
        targets(TargetFilter.CardInGraveyard, count = 2, optional = true)
        effect = Effects.Pipeline {
            val exiled = moveTracked(gather(CardSource.ChosenTargets), CardDestination.ToZone(Zone.EXILE))
            val creatures = filter(exiled, GameObjectFilter.Creature)
            run(Effects.GainLife(creatures.count))
        }
    }

    sagaChapter(3) {
        val card = target(TargetFilter(GameObjectFilter.CreatureOrLand.ownedByYou(), zone = Zone.GRAVEYARD))
        effect = Effects.Move(card, Zone.HAND)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "166"
        artist = "Adam Paquette"
        imageUri = "https://cards.scryfall.io/normal/front/4/6/46fb94ad-f9be-48b4-b65a-4e736e5ffdbb.jpg?1783931542"
    }
}
