package com.wingedsheep.mtg.sets.definitions.stx.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Bookwurm — Strixhaven: School of Mages #123 (canonical printing)
 * {7}{G} · Creature — Wurm · 7/7
 *
 * Trample
 * When this creature enters, you gain 3 life and draw a card.
 * {2}{G}: Put this card from your graveyard into your library third from the top.
 *
 * The last ability is activated from the graveyard (`activateFromZone = Zone.GRAVEYARD`) and
 * resolves to [Effects.PutIntoLibraryNthFromTop] at index 2 (third from the top). If the card
 * leaves the graveyard in response it is a new object (CR 400.7), so `Self` no longer names it and
 * the move does nothing; with fewer than two cards in the library the card simply goes to the
 * bottom — both per the rulings.
 */
val Bookwurm = card("Bookwurm") {
    manaCost = "{7}{G}"
    colorIdentity = "G"
    typeLine = "Creature — Wurm"
    oracleText = "Trample\n" +
        "When this creature enters, you gain 3 life and draw a card.\n" +
        "{2}{G}: Put this card from your graveyard into your library third from the top."
    power = 7
    toughness = 7

    keywords(Keyword.TRAMPLE)

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.GainLife(3) then Effects.DrawCards(1)
    }

    activatedAbility {
        cost = Costs.Mana("{2}{G}")
        activateFromZone = Zone.GRAVEYARD
        effect = Effects.PutIntoLibraryNthFromTop(EffectTarget.Self, positionFromTop = 2)
        description = "{2}{G}: Put this card from your graveyard into your library third from the top."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "123"
        artist = "Wayne Reynolds"
        flavorText = "It takes \"devouring a book in one sitting\" to a frightening extreme."
        imageUri = "https://cards.scryfall.io/normal/front/5/7/57d230fc-d382-40b4-bdbd-5fe880fa16bf.jpg?1783927347"
        ruling("2021-04-16", "If Bookwurm is removed from your graveyard in response to its last ability, it won't be put into your library.")
        ruling("2021-04-16", "If you have zero or one card left in your library when Bookwurm's last ability resolves, it is put on the bottom of your library.")
    }
}
