package com.wingedsheep.mtg.sets.definitions.mkm.cards

import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.Triggers
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.EventPattern.OneOrMoreDealCombatDamageToPlayerEvent
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.GrantKeyword
import com.wingedsheep.sdk.scripting.TriggerBinding
import com.wingedsheep.sdk.scripting.TriggerSpec
import com.wingedsheep.sdk.scripting.effects.ConditionalEffect
import com.wingedsheep.sdk.scripting.effects.FaceDownMode
import com.wingedsheep.sdk.scripting.effects.TurnFaceUpEffect
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * Yarus, Roar of the Old Gods — Murders at Karlov Manor #245
 * {2}{R}{G} · Legendary Creature — Centaur Druid · 4/4
 *
 * Other creatures you control have haste.
 * Whenever one or more face-down creatures you control deal combat damage to a player, draw a card.
 * Whenever a face-down creature you control dies, return it to the battlefield face down under its
 * owner's control if it's a permanent card, then turn it face up.
 *
 * **Both trigger halves needed engine work, and one of them was a live bug.**
 *
 * *The damage half.* `TriggerDetector.detectCombatDamageBatchTriggers` used to skip any source
 * carrying a `FaceDownComponent` outright, in both the offensive and the defensive variant. That
 * guard pre-dated `PredicateEvaluator`'s face-down masking and had become strictly harmful: a
 * face-down permanent *is* a creature (CR 708.2), so it should satisfy an unfiltered
 * "creatures you control" batch trigger, and the filtered ones are already decided by the evaluator,
 * which hides subtypes, colors, mana value — and now name — behind `isFaceDown`. Removing it fixed
 * this card *and* Kastral, the Windcrested and friends, which quietly under-triggered whenever a
 * morphed or disguised creature connected.
 *
 * *The death half.* `EntitySnapshot` never captured face-down-ness, so `StatePredicate.IsFaceDown`
 * had no last-known arm and this trigger could never match: a card put into a graveyard is turned
 * face up (CR 708.4) and the battlefield entity is gone by trigger-gating time, so the live
 * `FaceDownComponent` the projected path reads no longer exists. `EntitySnapshot.wasFaceDown` plus
 * an `IsFaceDown`/`IsFaceUp` arm in `matchesStatePredicateForZoneChangeTrigger` is the LKI channel
 * (CR 608.2h), sitting alongside `wasSuspected` / `wasAttacking` / `wasEnchanted`.
 *
 * **The dies filter carries no `Creature` card predicate on purpose.** Every face-down permanent is
 * a 2/2 creature whatever card is under it, so `faceDown()` already implies "creature" — while
 * spelling `GameObjectFilter.Creature` would *narrow* the trigger wrongly: the zone-change path
 * evaluates card predicates against the **printed** type line, so a cloaked or manifested *land*
 * card dying would fail `IsCreature` even though a face-down land is exactly the case this ability
 * exists to handle.
 *
 * **"If it's a permanent card" is checked against the graveyard card, not last-known info.** By
 * resolution "it" is a face-up card in the graveyard, and `EntityMatches` on a non-battlefield
 * entity falls back to base `CardComponent` data — so `Permanent` reads the real printed types. An
 * LKI card-type condition would be wrong here: the last-known type line of *any* face-down
 * permanent is Creature, so an instant card cloaked onto the battlefield would sail through the
 * gate and then be stranded on the battlefield.
 *
 * The return is deliberately two steps, matching the oracle order. `FaceDownMode.HIDDEN` is the
 * right mode for the intermediate state: the card is not being disguised or cloaked, so it gets no
 * ward {2} and no turn-up procedure of its own (2024-02-02 ruling) — `TurnFaceUpEffect` then flips
 * it without a cost. Going face down and back up rather than straight to the battlefield face up is
 * what makes a disguise creature's "when this is turned face up" trigger fire on the way back.
 *
 * `fromZone = Zone.GRAVEYARD` is the "left the graveyard first" gate: a card that has already moved
 * on by the time this resolves stays where it is (2024-02-02 ruling) rather than being dragged back
 * from wherever it went.
 */
val YarusRoarOfTheOldGods = card("Yarus, Roar of the Old Gods") {
    manaCost = "{2}{R}{G}"
    colorIdentity = "RG"
    typeLine = "Legendary Creature — Centaur Druid"
    power = 4
    toughness = 4
    oracleText = "Other creatures you control have haste.\n" +
        "Whenever one or more face-down creatures you control deal combat damage to a player, " +
        "draw a card.\n" +
        "Whenever a face-down creature you control dies, return it to the battlefield face down " +
        "under its owner's control if it's a permanent card, then turn it face up."

    staticAbility {
        ability = GrantKeyword(
            keyword = Keyword.HASTE,
            filter = GroupFilter(GameObjectFilter.Creature.youControl()).other()
        )
    }

    triggeredAbility {
        trigger = TriggerSpec(
            OneOrMoreDealCombatDamageToPlayerEvent(
                sourceFilter = GameObjectFilter.Creature.faceDown()
            ),
            TriggerBinding.ANY
        )
        effect = Effects.DrawCards(1)
        description = "Whenever one or more face-down creatures you control deal combat damage " +
            "to a player, draw a card."
    }

    triggeredAbility {
        trigger = Triggers.leavesBattlefield(
            filter = GameObjectFilter.Any.youControl().faceDown(),
            to = Zone.GRAVEYARD,
            binding = TriggerBinding.ANY
        )
        effect = ConditionalEffect(
            condition = Conditions.EntityMatches(
                EffectTarget.TriggeringEntity,
                GameObjectFilter.Permanent
            ),
            effect = Effects.Move(
                target = EffectTarget.TriggeringEntity,
                destination = Zone.BATTLEFIELD,
                fromZone = Zone.GRAVEYARD,
                faceDown = FaceDownMode.HIDDEN
            ) then TurnFaceUpEffect(EffectTarget.TriggeringEntity)
        )
        description = "Whenever a face-down creature you control dies, return it to the " +
            "battlefield face down under its owner's control if it's a permanent card, then turn " +
            "it face up."
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "245"
        artist = "Dmitry Burmak"
        imageUri = "https://cards.scryfall.io/normal/front/3/2/326845a7-7502-4dc3-8f3e-867d6c84e931.jpg?1783912832"

        ruling(
            "2024-02-02",
            "Normally, combat damage is dealt all at the same time. In that case, Yarus, Roar of " +
                "the Old Gods's last ability triggers once for each player that face-down " +
                "creatures you control dealt combat damage to, regardless of how many creatures " +
                "were dealing that damage. If any of those face-down creatures have double strike " +
                "or first strike, the ability will trigger once for each player dealt damage in " +
                "each combat damage step."
        )
        ruling(
            "2024-02-02",
            "If Yarus, Roar of the Old Gods dies at the same time as one or more face-down " +
                "creatures you control, its last ability triggers for each of those creatures."
        )
        ruling(
            "2024-02-02",
            "If a face-down creature dies but the permanent card it becomes in the graveyard " +
                "leaves the graveyard before Yarus, Roar of the Old Gods's last ability resolves, " +
                "it will not return to the battlefield."
        )
        ruling(
            "2024-02-02",
            "If a permanent card that would be returned to the battlefield face down by Yarus, " +
                "Roar of the Old Gods's last ability can't be turned face up for some reason, " +
                "it'll still be returned to the battlefield, but it will stay face down. In that " +
                "case, it will be a 2/2 creature with no name, mana cost, or creature types. " +
                "Since it isn't disguised or cloaked, it won't have ward {2}."
        )
    }
}
