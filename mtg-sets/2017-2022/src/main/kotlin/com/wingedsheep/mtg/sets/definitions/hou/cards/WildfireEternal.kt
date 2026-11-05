package com.wingedsheep.mtg.sets.definitions.hou.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.CardSource
import com.wingedsheep.sdk.scripting.references.Player

/**
 * Wildfire Eternal — Hour of Devastation #109 (canonical printing)
 * {3}{R} · Creature — Zombie Jackal Cleric · 1/4
 *
 * Afflict 4 (Whenever this creature becomes blocked, defending player loses 4 life.)
 * Whenever this creature attacks and isn't blocked, you may cast an instant or sorcery spell from
 * your hand without paying its mana cost.
 *
 * Afflict is keyword-only (see `Afflict`). The unblocked trigger reuses Omnispell Adept's free-cast
 * pipeline: gather the hand, choose up to one instant or sorcery, and cast it during the trigger's
 * resolution — so a sorcery is cast mid-combat with its timing restriction ignored.
 */
val WildfireEternal = card("Wildfire Eternal") {
    manaCost = "{3}{R}"
    colorIdentity = "R"
    typeLine = "Creature — Zombie Jackal Cleric"
    power = 1
    toughness = 4
    oracleText = "Afflict 4 (Whenever this creature becomes blocked, defending player loses 4 life.)\n" +
        "Whenever this creature attacks and isn't blocked, you may cast an instant or sorcery spell from your hand without paying its mana cost."

    afflict(4)

    triggeredAbility {
        trigger = Triggers.self.attacksAndIsntBlocked()
        effect = Effects.Pipeline {
            val hand = gather(CardSource.FromZone(Zone.HAND, Player.You))
            val spellToCast = chooseSpell(
                from = hand,
                filter = GameObjectFilter.InstantOrSorcery,
                showAllCards = true,
                prompt = "You may cast an instant or sorcery spell from your hand without paying its mana cost",
                selectedLabel = "Cast for free"
            )
            run(Effects.CastFromCollectionWithoutPayingCost(spellToCast))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "109"
        artist = "Magali Villeneuve"
        imageUri = "https://cards.scryfall.io/normal/front/f/d/fd845479-619a-4262-b445-d93975c183b4.jpg"
        ruling("2017-07-14", "Wildfire Eternal's second ability resolves before combat damage is dealt, and you must cast a spell at that time if you wish to cast one without paying its mana cost. You can cast a sorcery during the declare blockers step this way.")
        ruling("2017-07-14", "If you cast a card \"without paying its mana cost,\" you can't pay any alternative costs. You can, however, pay additional costs. If the card has any mandatory additional costs, such as that of Tormenting Voice, those must be paid to cast the card.")
        ruling("2017-07-14", "If the card has {X} in its mana cost, you must choose 0 as the value of X when casting it without paying its mana cost.")
        ruling("2017-07-14", "An ability that triggers when something \"attacks and isn't blocked\" triggers in the declare blockers step after blockers are declared if (1) that creature is attacking and (2) no creatures are declared to block it. It will trigger even if that creature was put onto the battlefield attacking rather than having been declared as an attacker in the declare attackers step.")
        ruling("2017-07-14", "If multiple creatures block a creature with afflict, afflict triggers only once.")
        ruling("2017-07-14", "Afflict causes the defending player to lose life; it's not damage or combat damage.")
        ruling("2017-07-14", "Afflict resolves before combat damage is dealt. If this loss of life brings a player to 0 life or less, that player loses the game immediately. A blocking creature with lifelink won't deal combat damage in time to save that player.")
        ruling("2023-07-28", "If a creature is attacking a planeswalker, that planeswalker's controller is the defending player. If a creature is attacking a battle, that battle's protector is the defending player.")
    }
}
