package com.wingedsheep.mtg.sets.definitions.one.cards

import com.wingedsheep.sdk.core.Color
import com.wingedsheep.sdk.core.Keyword
import com.wingedsheep.sdk.core.Step
import com.wingedsheep.sdk.core.Zone
import com.wingedsheep.sdk.dsl.Conditions
import com.wingedsheep.sdk.dsl.Effects
import com.wingedsheep.sdk.dsl.card
import com.wingedsheep.sdk.model.Rarity
import com.wingedsheep.sdk.scripting.AttackerCountLimit
import com.wingedsheep.sdk.scripting.GameObjectFilter
import com.wingedsheep.sdk.scripting.effects.Chooser
import com.wingedsheep.sdk.scripting.filters.unified.GroupFilter
import com.wingedsheep.sdk.scripting.filters.unified.TargetFilter
import com.wingedsheep.sdk.scripting.references.Player
import com.wingedsheep.sdk.scripting.targets.EffectTarget

/**
 * The Eternal Wanderer — Phyrexia: All Will Be One #11
 * {4}{W}{W} · Legendary Planeswalker · Starting loyalty 5
 *
 * No more than one creature can attack The Eternal Wanderer each combat.
 * +1: Exile up to one target artifact or creature. Return that card to the battlefield under its
 *     owner's control at the beginning of that player's next end step.
 * 0: Create a 2/2 white Samurai creature token with double strike.
 * −4: For each player, choose a creature that player controls. Each player sacrifices all
 *     creatures they control not chosen this way.
 *
 * - The static is the per-defender axis of [AttackerCountLimit] narrowed to the Wanderer itself
 *   (`sourceItself()` — the validator evaluates the filter with the limiting permanent as source).
 * - The +1's return is a step-based delayed trigger gated to the exiled card's *owner's* turn
 *   (`fireOnPlayer = OwnerOf`), so it fires at that player's next end step — this turn's if the
 *   owner is the active player and the end step hasn't begun. Guarded on a target having been
 *   chosen: "up to one" with nothing chosen has no owner to schedule against.
 * - The −4 loops every player; the Wanderer's controller ([Chooser.SourceController]) picks one
 *   creature each player controls (not targeted — hexproof/ward don't matter), then everything
 *   not picked is sacrificed simultaneously.
 */
val TheEternalWanderer = card("The Eternal Wanderer") {
    manaCost = "{4}{W}{W}"
    colorIdentity = "W"
    typeLine = "Legendary Planeswalker"
    startingLoyalty = 5
    oracleText = "No more than one creature can attack The Eternal Wanderer each combat.\n" +
        "+1: Exile up to one target artifact or creature. Return that card to the battlefield " +
        "under its owner's control at the beginning of that player's next end step.\n" +
        "0: Create a 2/2 white Samurai creature token with double strike.\n" +
        "−4: For each player, choose a creature that player controls. Each player sacrifices all " +
        "creatures they control not chosen this way."

    staticAbility {
        ability = AttackerCountLimit(
            maxAttackers = 1,
            defenders = GroupFilter(GameObjectFilter.Permanent.sourceItself())
        )
    }

    loyaltyAbility(+1) {
        val permanent = target(TargetFilter.CreatureOrArtifact, optional = true)
        effect = Effects.If(
            condition = Conditions.TargetMatchesFilter(GameObjectFilter.Any, permanent),
            then = Effects.Exile(permanent) then Effects.CreateDelayedTrigger(
                step = Step.END,
                fireOnPlayer = EffectTarget.PlayerRef(Player.OwnerOf("that card")),
                effect = Effects.Move(permanent, Zone.BATTLEFIELD, fromZone = Zone.EXILE)
            )
        )
    }

    loyaltyAbility(0) {
        effect = Effects.CreateToken(
            power = 2,
            toughness = 2,
            colors = setOf(Color.WHITE),
            creatureTypes = setOf("Samurai"),
            keywords = setOf(Keyword.DOUBLE_STRIKE),
            imageUri = "https://cards.scryfall.io/normal/front/7/0/70750c90-3856-4d6d-923b-2ab91b1d7049.jpg?1783918170"
        )
    }

    loyaltyAbility(-4) {
        effect = Effects.Pipeline {
            val (kept) = forEachPlayerCollecting(Player.ActivePlayerFirst) {
                val creatures = gather(GameObjectFilter.Creature, player = Player.You)
                listOf(
                    chooseExactly(
                        1,
                        from = creatures,
                        chooser = Chooser.SourceController,
                        prompt = "Choose a creature this player keeps",
                        useTargetingUI = true
                    )
                )
            }
            val all = gather(GameObjectFilter.Creature)
            sacrifice(exclude(all, kept))
        }
    }

    metadata {
        rarity = Rarity.RARE
        collectorNumber = "11"
        artist = "Alix Branwyn"
        imageUri = "https://cards.scryfall.io/normal/front/9/0/905f0dd4-0197-45f8-8e7f-396d6dcef600.jpg?1783918081"
        ruling("2023-02-04", "The Eternal Wanderer does not have a planeswalker type.")
        ruling("2023-02-04", "For the first loyalty ability, the exiled card will return to the battlefield at the beginning of that player's next end step even if The Eternal Wanderer is no longer on the battlefield at that time.")
        ruling("2023-02-04", "If the permanent that returns to the battlefield has any abilities that trigger at the beginning of the end step, those abilities won't trigger that turn.")
        ruling("2023-02-04", "Auras attached to the exiled permanent will be put into their owners' graveyards. Equipment attached to the exiled permanent will become unattached and remain on the battlefield. Any counters on the exiled permanent will cease to exist. Once the exiled permanent returns, it's considered a new object with no relation to the object that it was.")
        ruling("2023-02-04", "If a token is exiled this way, it will cease to exist and won't return to the battlefield.")
        ruling("2023-02-04", "For the last loyalty ability, none of the chosen creatures are targets of the ability. You may choose creatures with hexproof, for example, and choosing a creature with ward will not cause the ward ability to trigger.")
    }
}
