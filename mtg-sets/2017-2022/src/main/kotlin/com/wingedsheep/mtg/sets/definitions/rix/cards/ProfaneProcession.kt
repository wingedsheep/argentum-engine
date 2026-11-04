package com.wingedsheep.mtg.sets.definitions.rix.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.DynamicAmounts
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TimingRule
import com.wingedsheep.sdk.scripting.conditions.ComparisonOperator
import com.wingedsheep.sdk.scripting.effects.CardDestination
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Profane Procession // Tomb of the Dusk Rose (Rivals of Ixalan — the card's earliest printing)
 * {1}{W}{B}
 * Legendary Enchantment // Legendary Land
 *
 * Front — Profane Procession (Legendary Enchantment, {1}{W}{B})
 *   {3}{W}{B}: Exile target creature. Then if there are three or more cards exiled with Profane
 *   Procession, transform it.
 *
 * Back — Tomb of the Dusk Rose (Legendary Land)
 *   {T}: Add one mana of any color.
 *   {2}{W}{B}, {T}: Put a creature card exiled with this permanent onto the battlefield under
 *   your control.
 *
 * Implementation:
 *  - The exile links the card to the permanent (`linkToSource`, CR 607), so the count and the
 *    Tomb's reanimation see only what Profane Procession's own ability exiled (third ruling).
 *    Transforming keeps the same object, so the link carries over to the Tomb.
 *  - "Cards exiled with it" counts nontoken objects still in exile that this permanent exiled —
 *    an exiled token is still in exile until state-based actions, but isn't a card (first
 *    ruling). Read on resolution, after this activation's exile.
 *  - A second activation resolving after the transform doesn't flip the Tomb back: the
 *    transform engine ignores a self-transform from an ability put on the stack before the
 *    permanent last turned over (the "already transformed" rule, second ruling); its exile
 *    still feeds the Tomb.
 *  - The Tomb's ability chooses on resolution (no target) among the creature cards in its
 *    linked exile and puts it onto the battlefield under the activator's control.
 */

private val cardsExiledWithIt = GameObjectFilter.Any.nontoken().exiledWithSource()

private val ProfaneProcessionFront = card("Profane Procession") {
    manaCost = "{1}{W}{B}"
    colorIdentity = "WB"
    typeLine = "Legendary Enchantment"
    oracleText = "{3}{W}{B}: Exile target creature. Then if there are three or more cards exiled " +
        "with Profane Procession, transform it."

    activatedAbility {
        cost = Costs.Mana("{3}{W}{B}")
        val creature = target(TargetFilter.Creature)
        effect = Effects.Move(creature, Zone.EXILE, linkToSource = true) then
            Effects.If(
                condition = Conditions.CompareAmounts(
                    left = DynamicAmounts.count(Player.Each, Zone.EXILE, cardsExiledWithIt),
                    operator = ComparisonOperator.GTE,
                    right = 3,
                ),
                then = Effects.Transform(EffectTarget.Self),
            )
        description = "{3}{W}{B}: Exile target creature. Then if there are three or more cards " +
            "exiled with Profane Procession, transform it."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Bastien L. Deharme"
        flavorText = "Vona sought a conqueror. Mavren sought a savior. Both found what they sought . . ."
        imageUri = "https://cards.scryfall.io/normal/front/1/d/1d94ff37-f04e-48ee-8253-d62ab07f0632.jpg?1783935277"
        ruling("2018-01-19", "Profane Procession's ability can exile token creatures. They won't count towards the number of cards exiled with it.")
        ruling("2018-01-19", "If you have enough mana, you can activate Profane Procession's ability multiple times before any activation resolves. It will be transformed if one activation exiles the third card. Further activations waiting to resolve won't cause Tomb of the Dusk Rose to transform back into Profane Procession, but they will exile creatures that Tomb of the Dusk Rose can bring back.")
        ruling("2018-01-19", "If Profane Procession or Tomb of the Dusk Rose somehow has exiled cards other than with Profane Procession's ability, those exiled cards aren't linked to Tomb of the Dusk Rose's second ability. They can't be put onto the battlefield with that ability.")
    }
}

private val TombOfTheDuskRose = card("Tomb of the Dusk Rose") {
    manaCost = ""
    colorIdentity = "WB"
    typeLine = "Legendary Land"
    oracleText = "(Transforms from Profane Procession.)\n" +
        "{T}: Add one mana of any color.\n" +
        "{2}{W}{B}, {T}: Put a creature card exiled with this permanent onto the battlefield " +
        "under your control."

    activatedAbility {
        cost = Costs.Tap
        effect = Effects.AddAnyColorMana()
        manaAbility = true
        timing = TimingRule.ManaAbility
    }

    activatedAbility {
        cost = Costs.Composite(Costs.Mana("{2}{W}{B}"), Costs.Tap)
        effect = Effects.Pipeline {
            val exiled = gather(CardSource.FromLinkedExile())
            val chosen = chooseExactly(
                1,
                from = exiled,
                filter = GameObjectFilter.Creature,
                prompt = "Choose a creature card exiled with Tomb of the Dusk Rose",
                selectedLabel = "Put onto the battlefield",
            )
            move(chosen, CardDestination.ToZone(Zone.BATTLEFIELD, Player.You))
        }
        description = "{2}{W}{B}, {T}: Put a creature card exiled with this permanent onto the " +
            "battlefield under your control."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "166"
        artist = "Bastien L. Deharme"
        flavorText = ". . . resting in the splendor of the dead."
        imageUri = "https://cards.scryfall.io/normal/back/1/d/1d94ff37-f04e-48ee-8253-d62ab07f0632.jpg?1783935277"
    }
}

val ProfaneProcession: CardDefinition = CardDefinition.doubleFacedPermanent(
    frontFace = ProfaneProcessionFront,
    backFace = TombOfTheDuskRose,
)
