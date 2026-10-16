package com.wingedsheep.mtg.sets.definitions.pip.cards

import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.predicates.StatePredicate
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Dogmeat, Ever Loyal — Fallout #2
 * {R}{G}{W} · Legendary Creature — Dog · 3/3
 *
 * When Dogmeat enters, mill five cards, then return an Aura or Equipment card from your graveyard
 * to your hand.
 * Whenever a creature you control that's enchanted or equipped attacks, create a Junk token.
 *
 * - The mill runs first, so a just-milled Aura or Equipment is a legal pick (ruling). The return
 *   doesn't target and isn't optional: a `chooseExactly(1)` over the graveyard's Aura/Equipment
 *   cards, which returns nothing only when there are none (the Roots of Wisdom shape).
 * - The attack trigger fires once per attacking creature that is enchanted or equipped at the
 *   moment it's declared as an attacker.
 */
private val enchantedOrEquippedCreatureYouControl = GameObjectFilter.Creature.youControl()
    .withStatePredicate(StatePredicate.Or(listOf(StatePredicate.IsEnchanted, StatePredicate.IsEquipped)))

private val auraOrEquipmentCard =
    GameObjectFilter.Any.withSubtype(Subtype.AURA) or GameObjectFilter.Any.withSubtype(Subtype.EQUIPMENT)

val DogmeatEverLoyal = card("Dogmeat, Ever Loyal") {
    manaCost = "{R}{G}{W}"
    colorIdentity = "RGW"
    typeLine = "Legendary Creature — Dog"
    power = 3
    toughness = 3
    oracleText = "When Dogmeat enters, mill five cards, then return an Aura or Equipment card from your " +
        "graveyard to your hand.\n" +
        "Whenever a creature you control that's enchanted or equipped attacks, create a Junk token. " +
        "(It's an artifact with \"{T}, Sacrifice this token: Exile the top card of your library. " +
        "You may play that card this turn. Activate only as a sorcery.\")"

    triggeredAbility {
        trigger = Triggers.self.enters()
        effect = Effects.Pipeline {
            mill(5)
            val pool = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, auraOrEquipmentCard))
            val returned = chooseExactly(
                1,
                from = pool,
                prompt = "Return an Aura or Equipment card from your graveyard to your hand",
                selectedLabel = "Return to hand"
            )
            toHand(returned)
        }
    }

    triggeredAbility {
        trigger = Triggers.a(enchantedOrEquippedCreatureYouControl).attacks()
        effect = Effects.CreateJunk()
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "2"
        artist = "Kieran Yanner"
        imageUri = "https://cards.scryfall.io/normal/front/8/6/86b45e3e-8460-4678-87d1-d74479936c83.jpg?1783912422"
        ruling("2024-03-08", "The card you return with Dogmeat's first ability doesn't have to be one of the cards you milled with that ability.")
        ruling("2024-03-08", "You can't sacrifice a Junk token to pay multiple costs. For example, you can't sacrifice a Junk token to activate its own ability and also to activate the second ability of Junk Jet.")
        ruling("2024-03-08", "You pay all costs and follow all normal timing rules for the card played from exile with a Junk token's ability. For example, if the exiled card is a land card, you may play it only during your main phase while the stack is empty.")
        ruling("2024-03-08", "Some spells and abilities that create Junk tokens may require targets. If each target chosen is illegal as that spell or ability tries to resolve, it won't resolve and none of its effects happen. You won't create any Junk tokens.")
    }
}
