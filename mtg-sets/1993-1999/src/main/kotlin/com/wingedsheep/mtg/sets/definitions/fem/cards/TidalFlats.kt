package com.wingedsheep.mtg.sets.definitions.fem.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.dsl.Costs
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.PayOrSufferEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Tidal Flats
 * {U}
 * Enchantment
 * {U}{U}: For each attacking creature without flying, its controller may pay {1}. If that player
 * doesn't, creatures you control blocking that creature gain first strike until end of turn.
 *
 * [Heroism]'s shape in blue: a per-creature toll routed to each attacker's *own* controller
 * ([Player.ControllerOfIterationEntity]), not to Tidal Flats'. The payoff is asymmetric — the toll
 * is paid by the attacker, the first strike goes to *your* blockers — so the "you" of the suffer
 * half stays Tidal Flats' controller while the "its controller" of the payment does not.
 *
 * "Blocking that creature" means the loop's current attacker, which is what
 * `blockingIterationEntity()` reads; a source-relative filter would name the enchantment instead.
 */
val TidalFlats = card("Tidal Flats") {
    manaCost = "{U}"
    colorIdentity = "U"
    typeLine = "Enchantment"
    oracleText = "{U}{U}: For each attacking creature without flying, its controller may pay {1}. " +
        "If that player doesn't, creatures you control blocking that creature gain first strike " +
        "until end of turn."

    activatedAbility {
        cost = Costs.Mana("{U}{U}")
        effect = Effects.ForEachInGroup(
            filter = GroupFilter(GameObjectFilter.Creature.attacking().withoutKeyword(Keyword.FLYING)),
            effect = PayOrSufferEffect(
                cost = Costs.pay.Mana("{1}"),
                suffer = Patterns.Group.grantKeywordToAll(
                    Keyword.FIRST_STRIKE,
                    GroupFilter(GameObjectFilter.Creature.youControl().blockingIterationEntity())
                ),
                player = EffectTarget.PlayerRef(Player.ControllerOfIterationEntity),
                consequenceDescription = "let the creatures blocking it gain first strike this turn",
            )
        )
        description = "{U}{U}: For each attacking creature without flying, its controller may pay {1}. If that player doesn't, creatures you control blocking that creature gain first strike until end of turn."
    }

    metadata {
        rarity = Rarity.COMMON
        collectorNumber = "27a"
        artist = "Rob Alexander"
        imageUri = "https://cards.scryfall.io/normal/front/2/e/2e820f3f-434e-4d09-91b9-0ebd6966b393.jpg?1783947909"
    }
}
