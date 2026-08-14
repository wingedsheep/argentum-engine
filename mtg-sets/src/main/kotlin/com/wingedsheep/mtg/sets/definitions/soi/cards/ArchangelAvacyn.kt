package com.wingedsheep.mtg.sets.definitions.soi.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Subtype
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Filters
import com.wingedsheep.sdk.dsl.Patterns
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.CardDefinition
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.CreateDelayedTriggerEffect
import com.wingedsheep.sdk.scripting.effects.DealDamageEffect
import com.wingedsheep.sdk.scripting.effects.TransformEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget
import com.wingedsheep.sdk.core.Zone

/**
 * Archangel Avacyn // Avacyn, the Purifier (Shadows over Innistrad #5 — the card's earliest
 * printing; also reprinted in Shadows over Innistrad Remastered and Innistrad Remastered)
 * {3}{W}{W}
 * Legendary Creature — Angel 4/4 // Legendary Creature — Angel 6/5
 *
 * Front — Archangel Avacyn ({3}{W}{W}, Legendary Creature — Angel, 4/4)
 *   Flash
 *   Flying, vigilance
 *   When Archangel Avacyn enters, creatures you control gain indestructible until end of turn.
 *   When a non-Angel creature you control dies, transform Archangel Avacyn at the beginning of
 *   the next upkeep.
 *
 * Back — Avacyn, the Purifier (Legendary Creature — Angel, 6/5, red)
 *   Flying
 *   When this creature transforms into Avacyn, the Purifier, it deals 3 damage to each other
 *   creature and each opponent.
 *
 * Implementation:
 *  - The ETB grant is [Patterns.Group.grantKeywordToAll]`(INDESTRUCTIBLE, creaturesYouControl)`.
 *    It is a one-shot until-end-of-turn grant over the creatures present as it resolves, which is
 *    what the printed wording says — creatures entering later are not covered.
 *  - "When a non-Angel creature you control dies" is a [Triggers.leavesBattlefield] factory trigger
 *    with an ANY binding over `Creature.youControl().notSubtype(ANGEL)`. Avacyn herself is an Angel,
 *    so she never triggers her own flip.
 *  - "…transform Archangel Avacyn at the beginning of the next upkeep" is a step-based
 *    [CreateDelayedTriggerEffect] on [Step.UPKEEP] with no `fireOnPlayer`, so it fires at the next
 *    upkeep whatever turn that is (printed ruling).
 *  - The delayed effect is gated on the permanent still being named `Archangel Avacyn`
 *    ([Conditions.SourceMatches]). This is the second printed ruling: if several non-Angel creatures
 *    died in one turn, each death queues its own delayed trigger, and the ones that resolve after
 *    the first must **not** flip her back to the front face. Checking the face by name is exactly
 *    "is she still Archangel Avacyn?".
 *  - The back's trigger is [Triggers.TransformsToBack] — it fires only on the front→back flip, never
 *    when some other effect turns her face up again. Its damage is
 *    [Effects.ForEachInGroup]`(`[GroupFilter.AllOtherCreatures]`, …)` — `excludeSelf` keeps Avacyn
 *    out of her own sweep — followed by 3 damage to [Player.EachOpponent]. Damage is sourced from
 *    the ability's source, so it is Avacyn dealing it (lifelink/deathtouch grants, damage
 *    redirection and "damage from a source you control" triggers all see her).
 */

private val NonAngelCreatureYouControl: GameObjectFilter =
    GameObjectFilter.Creature.youControl().notSubtype(Subtype.ANGEL)

private val ArchangelAvacynFront = card("Archangel Avacyn") {
    manaCost = "{3}{W}{W}"
    colorIdentity = "RW" // Card-level identity: the red back face contributes (CR 903.4).
    typeLine = "Legendary Creature — Angel"
    power = 4
    toughness = 4
    oracleText = "Flash\n" +
        "Flying, vigilance\n" +
        "When Archangel Avacyn enters, creatures you control gain indestructible until end of turn.\n" +
        "When a non-Angel creature you control dies, transform Archangel Avacyn at the beginning " +
        "of the next upkeep."

    keywords(Keyword.FLASH, Keyword.FLYING, Keyword.VIGILANCE)

    triggeredAbility {
        trigger = Triggers.EntersBattlefield
        effect = Patterns.Group.grantKeywordToAll(
            Keyword.INDESTRUCTIBLE,
            Filters.Group.creaturesYouControl,
        )
        description = "Creatures you control gain indestructible until end of turn."
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = NonAngelCreatureYouControl,
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY,
        )
        effect = CreateDelayedTriggerEffect(
            step = Step.UPKEEP,
            effect = ConditionalEffect(
                condition = Conditions.SourceMatches(
                    GameObjectFilter.Any.named("Archangel Avacyn"),
                ),
                effect = TransformEffect(EffectTarget.Self),
            ),
        )
        description = "Transform Archangel Avacyn at the beginning of the next upkeep."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "James Ryman"
        imageUri = "https://cards.scryfall.io/normal/front/a/e/ae155ee2-008f-4dc6-82bf-476be7baa224.jpg?1783937831"
        ruling(
            "2016-04-08",
            "Archangel Avacyn's delayed triggered ability triggers at the beginning of the next " +
                "upkeep regardless of whose turn it is."
        )
        ruling(
            "2016-04-08",
            "Archangel Avacyn's delayed triggered ability won't cause it to transform back into " +
                "Archangel Avacyn if it has already transformed into Avacyn, the Purifier, perhaps " +
                "because several creatures died in one turn."
        )
    }
}

private val AvacynThePurifier = card("Avacyn, the Purifier") {
    manaCost = ""
    colorIdentity = "RW"
    colorIndicator = "R" // Transformed back face, no mana cost (CR 204).
    typeLine = "Legendary Creature — Angel"
    power = 6
    toughness = 5
    oracleText = "Flying\n" +
        "When this creature transforms into Avacyn, the Purifier, it deals 3 damage to each " +
        "other creature and each opponent."

    keywords(Keyword.FLYING)

    triggeredAbility {
        trigger = Triggers.TransformsToBack
        effect = Effects.ForEachInGroup(
            GroupFilter.AllOtherCreatures,
            DealDamageEffect(3, EffectTarget.Self),
        ) then Effects.DealDamage(3, EffectTarget.PlayerRef(Player.EachOpponent))
        description = "Avacyn, the Purifier deals 3 damage to each other creature and each opponent."
    }

    metadata {
        rarity = Rarity.MYTHIC
        collectorNumber = "5"
        artist = "James Ryman"
        flavorText = "\"Wings that once bore hope are now stained with blood. She is our guardian " +
            "no longer.\"\n—Grete, cathar apostate"
        imageUri = "https://cards.scryfall.io/normal/back/a/e/ae155ee2-008f-4dc6-82bf-476be7baa224.jpg?1783937831"
    }
}

val ArchangelAvacyn: CardDefinition = CardDefinition.doubleFacedCreature(
    frontFace = ArchangelAvacynFront,
    backFace = AvacynThePurifier,
)
