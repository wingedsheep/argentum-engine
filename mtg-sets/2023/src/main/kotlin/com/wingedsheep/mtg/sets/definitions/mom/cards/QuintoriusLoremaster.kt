package com.wingedsheep.mtg.sets.definitions.mom.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.AfterResolveDestination
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter

/**
 * Quintorius, Loremaster {3}{R}{W}
 * Legendary Creature — Elephant Cleric
 * 3/5
 * Vigilance
 * At the beginning of your end step, exile target noncreature, nonland card from your graveyard.
 * Create a 3/2 red and white Spirit creature token.
 * {1}{R}{W}, {T}, Sacrifice a Spirit: Choose target card exiled with Quintorius. You may cast
 * that card this turn without paying its mana cost. If that spell would be put into a graveyard,
 * put it on the bottom of its owner's library instead.
 *
 * The two abilities are linked through the source's linked-exile pile: the trigger exiles with
 * `ExileLinkedToSource`, and the activation targets `exiledWithSource()` in exile — so a Spirit
 * exiled by a replacement effect when sacrificed is never a legal target (the linked-abilities
 * ruling). The cast is a lingering "this turn" permission, not a cast during resolution, and the
 * bottom-of-library rider rides on the card as `insteadOfGraveyard`.
 */
val QuintoriusLoremaster = card("Quintorius, Loremaster") {
    manaCost = "{3}{R}{W}"
    colorIdentity = "RW"
    typeLine = "Legendary Creature — Elephant Cleric"
    power = 3
    toughness = 5
    oracleText = "Vigilance\n" +
        "At the beginning of your end step, exile target noncreature, nonland card from your graveyard. " +
        "Create a 3/2 red and white Spirit creature token.\n" +
        "{1}{R}{W}, {T}, Sacrifice a Spirit: Choose target card exiled with Quintorius. You may cast that " +
        "card this turn without paying its mana cost. If that spell would be put into a graveyard, put it " +
        "on the bottom of its owner's library instead."

    keywords(Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.you.beginningOf(Step.END)
        val card = target(
            TargetFilter(GameObjectFilter.Noncreature and GameObjectFilter.Nonland, zone = Zone.GRAVEYARD).ownedByYou()
        )
        effect = Effects.ExileLinkedToSource(card) then
            Effects.CreateToken(
                power = 3,
                toughness = 2,
                colors = setOf(Color.RED, Color.WHITE),
                creatureTypes = setOf("Spirit"),
                imageUri = "https://cards.scryfall.io/normal/front/0/b/0ba7dada-ba7f-4233-ba21-f6f32698997a.jpg?1783916668"
            )
    }

    activatedAbility {
        cost = Costs.Composite(
            Costs.Mana("{1}{R}{W}"),
            Costs.Tap,
            Costs.Sacrifice(GameObjectFilter.Any.withSubtype("Spirit"))
        )
        val exiled = target(TargetFilter(GameObjectFilter.Any.exiledWithSource(), zone = Zone.EXILE))
        effect = Effects.GrantFreeCastTargetFromExile(
            target = exiled,
            insteadOfGraveyard = AfterResolveDestination.BOTTOM_OF_LIBRARY
        )
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "250"
        artist = "Lie Setiawan"
        imageUri = "https://cards.scryfall.io/normal/front/a/1/a19b152c-8376-42f9-9daf-e139dc29c9ca.jpg?1783916939"
        ruling("2023-04-14", "You must follow all applicable timing rules for casting the exiled card. For example, if the card is a sorcery card, you can cast it only during your main phase while the stack is empty.")
        ruling("2023-04-14", "Quintorius's last two abilities are linked. The last ability can target only cards exiled by the triggered ability. If a Spirit you sacrifice to activate the last ability ends up being exiled because of a replacement effect, that card can't be targeted by the last ability.")
        ruling("2023-04-14", "If the target of Quintorius's triggered ability is an illegal target at the time the ability tries to resolve, the ability won't resolve and none of its effects will happen. You won't create a Spirit token.")
        ruling("2023-04-14", "If you cast a card \"without paying its mana cost,\" you can't choose to cast it for any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, you must pay those to cast the card.")
        ruling("2023-04-14", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
    }
}
