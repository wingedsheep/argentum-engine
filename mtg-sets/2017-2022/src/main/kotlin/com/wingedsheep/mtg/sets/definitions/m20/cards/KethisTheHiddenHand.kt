package com.wingedsheep.mtg.sets.definitions.m20.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.CostModification
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.ModifySpellCost
import com.wingedsheep.sdk.scripting.SpellCostTarget
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Kethis, the Hidden Hand
 * {W}{B}{G}
 * Legendary Creature — Elf Advisor
 * 3/4
 * Legendary spells you cast cost {1} less to cast.
 * Exile two legendary cards from your graveyard: Until end of turn, each legendary card in your
 * graveyard gains "You may play this card from your graveyard."
 *
 * The cards are fixed as the ability resolves — a per-card play permission, not a static grant —
 * so a legendary card put into the graveyard later doesn't gain it, and one that is played and
 * comes back is a new object without it (the engine drops a card's permissions when it changes
 * zones).
 */
val KethisTheHiddenHand = card("Kethis, the Hidden Hand") {
    manaCost = "{W}{B}{G}"
    colorIdentity = "WBG"
    typeLine = "Legendary Creature — Elf Advisor"
    power = 3
    toughness = 4
    oracleText = "Legendary spells you cast cost {1} less to cast.\n" +
        "Exile two legendary cards from your graveyard: Until end of turn, each legendary card in your " +
        "graveyard gains \"You may play this card from your graveyard.\""

    staticAbility {
        ability = ModifySpellCost(
            target = SpellCostTarget.YouCast(GameObjectFilter.Any.legendary()),
            modification = CostModification.ReduceGeneric(1),
        )
    }

    activatedAbility {
        cost = Costs.ExileFromGraveyard(2, GameObjectFilter.Any.legendary())
        effect = Effects.Pipeline {
            val legends = gather(CardSource.FromZone(Zone.GRAVEYARD, Player.You, GameObjectFilter.Any.legendary()))
            run(Effects.GrantMayPlayFromExile(legends))
        }
        description = "Until end of turn, each legendary card in your graveyard gains " +
            "\"You may play this card from your graveyard.\""
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "211"
        artist = "Yongjae Choi"
        flavorText = "\"The pen is mightier than the crown.\""
        imageUri = "https://cards.scryfall.io/normal/front/f/e/fe28de73-76f3-4a9e-a020-dbe5921b9be5.jpg?1783932951"
        ruling("2019-07-12", "Kethis's last ability affects only legendary cards that are in your graveyard at the time it resolves. Legendary cards put into your graveyard later in the turn won't gain the ability. If you play a legendary card and it returns to your graveyard, it won't have the ability anymore.")
        ruling("2019-07-12", "Kethis's last ability doesn't change when you can play the legendary cards. For example, if you have a legendary creature card in your graveyard, you can cast it only during your main phase when the stack is empty. If you have a legendary land card in your graveyard, you can play it only during your main phase and only if you have an available land play remaining.")
        ruling("2019-07-12", "You'll still pay all costs for a legendary spell you cast this way, including additional costs. You may also pay alternative costs.")
    }
}
