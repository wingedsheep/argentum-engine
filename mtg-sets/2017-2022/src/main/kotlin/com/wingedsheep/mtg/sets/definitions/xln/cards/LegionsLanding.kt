package com.wingedsheep.mtg.sets.definitions.xln.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Legion's Landing // Adanto, the First Fort (Ixalan — the card's earliest printing)
 * {W}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Legion's Landing (Legendary Enchantment, {W})
 *   When Legion's Landing enters, create a 1/1 white Vampire creature token with lifelink.
 *   When you attack with three or more creatures, transform Legion's Landing.
 *
 * Back — Adanto, the First Fort (Legendary Land)
 *   {T}: Add {W}.
 *   {2}{W}, {T}: Create a 1/1 white Vampire creature token with lifelink.
 *
 * Implementation:
 *  - The flip is Overwhelming Instinct's `Triggers.you.attacks(minAttackers = 3)` — it counts
 *    only the creatures declared as attackers, so creatures put onto the battlefield attacking
 *    don't count, and once it triggers it transforms even if attackers later leave combat
 *    (both per the rulings) — wrapping [Effects.Transform] on Self.
 *  - The Vampire tokens carry no `imageUri`: XLN's token art resolves from the synced token sheet
 *    (Mavren Fein, Dusk Apostle makes the same token).
 */

private fun vampireToken() = Effects.CreateToken(
    power = 1,
    toughness = 1,
    colors = setOf(Color.WHITE),
    creatureTypes = setOf("Vampire"),
    keywords = setOf(Keyword.LIFELINK),
)

private val LegionsLandingFront = card("Legion's Landing") {
    manaCost = "{W}"
    typeLine = "Legendary Enchantment"
    oracleText = "When Legion's Landing enters, create a 1/1 white Vampire creature token with " +
        "lifelink.\n" +
        "When you attack with three or more creatures, transform Legion's Landing."

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = vampireToken()
        description = "When Legion's Landing enters, create a 1/1 white Vampire creature token " +
            "with lifelink."
    }

    triggeredAbility {
        trigger = Triggers.you.attacks(minAttackers = 3)
        effect = Effects.Transform(EffectTarget.Self)
        description = "When you attack with three or more creatures, transform Legion's Landing."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Svetlin Velinov"
        flavorText = "\"I claim this land in the name of the queen.\"\n—Adrian Adanto of Lujio"
        imageUri = "https://cards.scryfall.io/normal/front/0/5/05e2a5e6-3aaa-4096-bdd0-fcc1afe5a36c.jpg?1783935808"
        ruling(
            "2017-09-29",
            "Once you've attacked with three or more creatures, Legion's Landing will transform " +
                "even if some of those creatures leave the battlefield or are removed from combat.",
        )
        ruling(
            "2017-09-29",
            "The last ability of Legion's Landing only counts creatures that you declare as " +
                "attacking creatures. Creatures that enter the battlefield attacking won't count.",
        )
    }
}

private val AdantoTheFirstFort = card("Adanto, the First Fort") {
    manaCost = ""
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Legion's Landing.)\n" +
        "{T}: Add {W}.\n" +
        "{2}{W}, {T}: Create a 1/1 white Vampire creature token with lifelink."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddMana(Color.WHITE)
        manaAbility = true
        timing = TimingRule.ManaAbility
        description = "{T}: Add {W}."
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}"), Costs.Tap)
        effect = vampireToken()
        description = "{2}{W}, {T}: Create a 1/1 white Vampire creature token with lifelink."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "22"
        artist = "Svetlin Velinov"
        imageUri = "https://cards.scryfall.io/normal/back/0/5/05e2a5e6-3aaa-4096-bdd0-fcc1afe5a36c.jpg?1783935808"
    }
}

val LegionsLanding: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = LegionsLandingFront,
    backFace = AdantoTheFirstFort,
)
