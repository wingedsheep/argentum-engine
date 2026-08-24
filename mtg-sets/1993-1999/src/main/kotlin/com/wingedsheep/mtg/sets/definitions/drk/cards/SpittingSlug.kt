package com.wingedsheep.mtg.sets.definitions.drk.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Spitting Slug
 * {1}{G}{G}
 * Creature — Slug 2/4
 * Whenever this creature blocks or becomes blocked, you may pay {1}{G}. If you do, this creature
 * gains first strike until end of turn. Otherwise, each creature blocking or blocked by this
 * creature gains first strike until end of turn.
 *
 * "You may pay … if you do X, otherwise Y" is `PayOrSuffer` read the other way round: the printed
 * *suffer* branch here is handing first strike to the other side, and the reward for paying is
 * keeping it for yourself. So the pay-side grant runs first, unconditionally, and the suffer branch
 * takes it back off the Slug while giving it to its combat partners — the two branches are
 * mutually exclusive because `PayOrSuffer` only runs `suffer` when the cost went unpaid.
 *
 * Two pieces of vocabulary this needed:
 *  - The printed trigger has **no partner clause** ("blocks or becomes blocked"), so it is a single
 *    trigger however many creatures the Slug ends up paired with — `Triggers.BlocksOrBecomesBlocked`,
 *    which sets the once-per-combat firing mode — CR 509.3a/509.3c, "blocks" and "becomes blocked"
 *    each trigger only once per combat. `BlocksOrBecomesBlockedBy(filter)` (Corrosive Ooze, the
 *    509.3b/509.3d wordings) keeps firing per partner; here that would ask for {1}{G} once per
 *    blocker.
 *  - "Each creature blocking or blocked by this creature" is the *live* CR 509 pairing in both
 *    directions — `GameObjectFilter.Creature.blockingOrBlockedBySource()`. Abu Ja'far's
 *    `LastKnownCombatPairedWithSource` answers the same question from a leaves-battlefield snapshot,
 *    which is the wrong tool while the Slug is still on the battlefield.
 */
val SpittingSlug = card("Spitting Slug") {
    manaCost = "{1}{G}{G}"
    typeLine = "Creature — Slug"
    power = 2
    toughness = 4
    oracleText = "Whenever this creature blocks or becomes blocked, you may pay {1}{G}. If you " +
        "do, this creature gains first strike until end of turn. Otherwise, each creature " +
        "blocking or blocked by this creature gains first strike until end of turn."

    triggeredAbility {
        trigger = Triggers.BlocksOrBecomesBlocked()
        effect = Effects.Composite(
            // Runs whether or not the cost is paid; the unpaid branch below hands first strike to
            // the partners instead, so the Slug never keeps it for free.
            Effects.GrantKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self),
            PayOrSufferEffect(
                cost = Costs.pay.Mana("{1}{G}"),
                suffer = Effects.Composite(
                    Effects.RemoveKeyword(Keyword.FIRST_STRIKE, EffectTarget.Self),
                    Patterns.Group.grantKeywordToAll(
                        Keyword.FIRST_STRIKE,
                        GroupFilter(GameObjectFilter.Creature.blockingOrBlockedBySource())
                    ),
                ),
            ),
        )
        description = "Whenever this creature blocks or becomes blocked, you may pay {1}{G}. If " +
            "you do, this creature gains first strike until end of turn. Otherwise, each " +
            "creature blocking or blocked by this creature gains first strike until end of turn."
    }

    metadata {
        rarity = Rarity.UNCOMMON
        collectorNumber = "88"
        artist = "Anson Maddocks"
        imageUri = "https://cards.scryfall.io/normal/front/7/0/7011356e-7516-4ca0-ac54-d30af7ce03a2.jpg?1783947929"
    }
}
