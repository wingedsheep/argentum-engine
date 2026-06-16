package com.wingedsheep.mtg.sets.definitions.sos.cards

import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.KeywordAbility
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.targets.TargetObject

/**
 * Forum Necroscribe
 * {5}{B}
 * Creature — Troll Warlock
 * 5/4
 * Ward—Discard a card.
 * Repartee — Whenever you cast an instant or sorcery spell that targets a creature, return
 * target creature card from your graveyard to the battlefield.
 *
 * "Repartee" is an ability word (flavor only). The trigger is a standard
 * `youCastSpell(InstantOrSorcery)` narrowed by `CardPredicate.TargetsMatching(Creature)` so it
 * fires only when the cast spell targets a creature; it then reanimates a creature card from
 * your graveyard. (The trigger goes on the stack above the spell that caused it, so it can
 * resolve and reanimate before that spell does.)
 */
val ForumNecroscribe = card("Forum Necroscribe") {
    manaCost = "{5}{B}"
    colorIdentity = "B"
    typeLine = "Creature — Troll Warlock"
    power = 5
    toughness = 4
    oracleText = "Ward—Discard a card.\n" +
        "Repartee — Whenever you cast an instant or sorcery spell that targets a creature, " +
        "return target creature card from your graveyard to the battlefield."

    keywordAbility(KeywordAbility.wardDiscard())

    triggeredAbility {
        trigger = Triggers.youCastSpell(
            spellFilter = GameObjectFilter.InstantOrSorcery.targetsMatching(GameObjectFilter.Creature)
        )
        val returned = target(
            "target creature card from your graveyard",
            TargetObject(filter = TargetFilter.CreatureInYourGraveyard),
        )
        effect = Effects.Move(returned, Zone.BATTLEFIELD)
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "84"
        artist = "Randy Vargas"
        flavorText = "She had watched many arguments wither on the Forum floor. Now, it was " +
            "time to call on them to prove her point."
        imageUri = "https://cards.scryfall.io/normal/front/6/7/67504a12-7414-4209-bf1c-624b4db19d52.jpg?1775937497"
    }
}
