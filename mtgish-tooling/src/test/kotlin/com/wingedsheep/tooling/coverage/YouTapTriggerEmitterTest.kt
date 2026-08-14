package com.wingedsheep.tooling.coverage

import com.wingedsheep.tooling.coverage.emitter.Emitter
import io.kotest.core.spec.style.StringSpec
import io.kotest.matchers.shouldBe
import io.kotest.matchers.string.shouldContain
import io.kotest.matchers.string.shouldNotContain
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.add
import kotlinx.serialization.json.addJsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject

/**
 * Pins the emitter's recovery of `WhenAPlayerTapsAPermanent` — the tap-*attribution* trigger
 * ("Whenever you tap an untapped creature an opponent controls", Wilds of Eldraine's Hylda of the Icy
 * Crown / Icewrought Sentry / Solitary Sanctuary / Sharae of Numbing Depths) — onto
 * `Triggers.YouTap(filter)`.
 *
 * Two things are load-bearing and neither is visible in the corpus today (all four printed cards carry
 * payoffs the emitter still scaffolds on, so only a synthetic fixture exercises the trigger itself):
 *
 *  - The IR's explicit `IsUntapped` clause must be **dropped**, not round-tripped. The filter is
 *    evaluated when the trigger is detected — after the permanent has become tapped — so a recovered
 *    `.untapped()` predicate would read false and the trigger would never fire. The engine gets that
 *    half structurally: tapping is a transition (CR 603.2f), so an already-tapped permanent emits no
 *    tap event.
 *  - Only the `You` scope has a calibrated form; any other player scope must decline to a SCAFFOLD
 *    rather than be silently widened into "whenever *anyone* taps".
 *
 * Each fixture is a synthetic single-trigger card whose payoff (gain 1 life) renders whole, so the
 * only thing under test is the trigger. Hermetic: no IR download, no Scryfall cache.
 */
class YouTapTriggerEmitterTest : StringSpec({

    val effects = Registry.loadEffectSerialNames()
    val keywords = Registry.loadKeywords()

    /**
     * A "whenever [playerScope] taps a permanent matching [an untapped creature an opponent controls],
     * you gain 1 life" card. [includeIsUntapped] controls whether the filter carries the IR's
     * `IsUntapped` clause.
     */
    fun tapTriggerCard(
        playersScope: String,
        playerArg: String?,
        includeIsUntapped: Boolean,
    ): JsonObject = buildJsonObject {
        put("Name", JsonPrimitive("Test You Tap $playersScope${playerArg?.let { " $it" } ?: ""}"))
        putJsonObject("Typeline") {
            putJsonArray("Supertypes") {}
            putJsonArray("Cardtypes") { add(JsonPrimitive("Enchantment")) }
            putJsonArray("Subtypes") {}
        }
        putJsonArray("ManaCost") { addJsonObject { put("_ManaSymbol", JsonPrimitive("ManaCostW")) } }
        putJsonArray("Rules") {
            addJsonObject {
                put("_Rule", JsonPrimitive("TriggerA"))
                putJsonArray("args") {
                    addJsonObject {
                        put("_Trigger", JsonPrimitive("WhenAPlayerTapsAPermanent"))
                        putJsonArray("args") {
                            // arg 0: who did the tapping.
                            addJsonObject {
                                put("_Players", JsonPrimitive(playersScope))
                                if (playerArg != null) {
                                    putJsonObject("args") { put("_Player", JsonPrimitive(playerArg)) }
                                }
                            }
                            // arg 1: which permanents count.
                            addJsonObject {
                                put("_Permanents", JsonPrimitive("And"))
                                putJsonArray("args") {
                                    if (includeIsUntapped) {
                                        addJsonObject { put("_Permanents", JsonPrimitive("IsUntapped")) }
                                    }
                                    addJsonObject {
                                        put("_Permanents", JsonPrimitive("IsCardtype"))
                                        put("args", JsonPrimitive("Creature"))
                                    }
                                    addJsonObject {
                                        put("_Permanents", JsonPrimitive("ControlledByAPlayer"))
                                        putJsonObject("args") { put("_Players", JsonPrimitive("Opponent")) }
                                    }
                                }
                            }
                        }
                    }
                    addJsonObject {
                        put("_Actions", JsonPrimitive("ActionList"))
                        putJsonArray("args") {
                            addJsonObject {
                                put("_Action", JsonPrimitive("GainLife"))
                                putJsonArray("args") {
                                    addJsonObject {
                                        put("_Players", JsonPrimitive("SinglePlayer"))
                                        putJsonObject("args") { put("_Player", JsonPrimitive("You")) }
                                    }
                                    addJsonObject {
                                        put("_GameNumber", JsonPrimitive("Integer"))
                                        put("args", JsonPrimitive(1))
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    fun render(playersScope: String, playerArg: String?, includeIsUntapped: Boolean = true) =
        Emitter.renderCard(
            tapTriggerCard(playersScope, playerArg, includeIsUntapped), null, effects, keywords
        )

    "the You scope maps to Triggers.YouTap with the recovered permanent filter" {
        val r = render("SinglePlayer", "You")
        r.complete shouldBe true
        r.text shouldContain "trigger = Triggers.YouTap(GameObjectFilter.Creature.opponentControls())"
    }

    "the IR's IsUntapped clause is dropped — it would invert the filter at detection time" {
        val r = render("SinglePlayer", "You")
        r.text shouldNotContain "untapped()"
    }

    "the filter still renders when the IR omits IsUntapped" {
        val r = render("SinglePlayer", "You", includeIsUntapped = false)
        r.complete shouldBe true
        r.text shouldContain "trigger = Triggers.YouTap(GameObjectFilter.Creature.opponentControls())"
    }

    "a non-You tapper scope declines to a scaffold rather than widening the trigger" {
        for (scope in listOf("AnyPlayer" to null, "Opponent" to null, "SinglePlayer" to "HostController")) {
            val r = render(scope.first, scope.second)
            r.complete shouldBe false
            r.text shouldNotContain "Triggers.YouTap"
        }
    }
})
