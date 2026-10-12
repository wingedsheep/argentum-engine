package com.wingedsheep.mtg.sets.definitions.bro.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter

/**
 * Ashnod, Flesh Mechanist
 * {B}
 * Legendary Creature — Human Artificer
 * 1/1
 * Deathtouch
 * Whenever Ashnod attacks, you may sacrifice another creature. If you do, create a tapped Powerstone token.
 * {5}, Exile a creature card from your graveyard: Create a tapped 3/3 colorless Zombie artifact creature token.
 *
 * The attack trigger mirrors Vampire Gourmand: an [Effects.MayPay] whose optional cost is the
 * sacrifice of another creature (`excludeSource`), so the Powerstone is created only if you do.
 */
val AshnodFleshMechanist = card("Ashnod, Flesh Mechanist") {
    manaCost = "{B}"
    colorIdentity = "B"
    typeLine = "Legendary Creature — Human Artificer"
    power = 1
    toughness = 1
    oracleText = "Deathtouch\n" +
        "Whenever Ashnod attacks, you may sacrifice another creature. If you do, create a tapped Powerstone token.\n" +
        "{5}, Exile a creature card from your graveyard: Create a tapped 3/3 colorless Zombie artifact creature token."

    keywords(Keyword.DEATHTOUCH)

    triggeredAbility {
        trigger = Triggers.self.attacks()
        effect = Effects.MayPay(
            Effects.SacrificeOwn(GameObjectFilter.Creature, excludeSource = true),
            then = Effects.CreatePowerstone(tapped = true)
        )
        description = "Whenever Ashnod attacks, you may sacrifice another creature. If you do, create a tapped Powerstone token."
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{5}"),
            Costs.ExileFromGraveyard(1, GameObjectFilter.Creature)
        )
        effect = Effects.CreateToken(
            power = 3,
            toughness = 3,
            creatureTypes = setOf("Zombie"),
            artifactToken = true,
            tapped = true,
        )
        description = "{5}, Exile a creature card from your graveyard: Create a tapped 3/3 colorless Zombie artifact creature token."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "84"
        artist = "Howard Lyon"
        imageUri = "https://cards.scryfall.io/normal/front/5/e/5eb43c15-50a9-488f-b0be-84a5e0a6d10b.jpg?1783920096"
    }
}
